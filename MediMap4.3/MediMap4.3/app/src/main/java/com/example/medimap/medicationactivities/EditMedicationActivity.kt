package com.example.medimap.medicationactivities

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.medimap.MainActivity
import com.example.medimap.MedicationViewModel
import com.example.medimap.MedicationViewModelFactory
import com.example.medimap.R
import com.example.medimap.database.AppDatabase
import com.example.medimap.database.Medication
import com.example.medimap.receiver.MedicationReminderReceiver
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.Calendar

class EditMedicationActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var dosageEditText: TextInputEditText
    private lateinit var timingEditText: TextInputEditText // Now for reminder time

    private lateinit var selectTimeButton: Button
    private lateinit var selectDaysPicker: TextView

    private lateinit var saveButton: Button
    private lateinit var medicationViewModel: MedicationViewModel
    private var medicationId: Int = -1 // Default value indicating no ID yet

    private val REMINDER_CHANNEL_ID = "medication_channel_id" // Define a channel ID for reminders
    private val EDIT_NOTIFICATION_ID_BASE = 2000 // Base ID for the "edited" notification

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medication)

        // Initialize UI elements
        nameEditText = findViewById(R.id.medication_name_edit_text)
        dosageEditText = findViewById(R.id.medication_dosage_edit_text)
        timingEditText = findViewById(R.id.medication_timing_edit_text)
        selectTimeButton = findViewById(R.id.select_time_button)
        selectDaysPicker = findViewById(R.id.select_days_picker)
        saveButton = findViewById(R.id.save_medication_button)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize the ViewModel
        val database = AppDatabase.Companion.getInstance(applicationContext)
        val medicationDao = database.medicationDao()
        val viewModelFactory = MedicationViewModelFactory(medicationDao)
        medicationViewModel = ViewModelProvider(this, viewModelFactory).get(MedicationViewModel::class.java)

        // Get the medication ID passed from the previous screen
        medicationId = intent.getIntExtra("MEDICATION_ID", -1)
        Log.d("EditMedication", "Editing medication with ID: $medicationId")

        // Observe the medication details and update the UI
        medicationViewModel.getMedicationById(medicationId)
            .observe(this, Observer { medication ->
                medication?.let {
                    Log.d("EditMedication", "Loaded medication: ${it.name}")
                    supportActionBar?.title = "Edit ${it.name}"
                    nameEditText.setText(it.name)
                    dosageEditText.setText(it.dosage)
                    timingEditText.setText(it.reminderTime)
                    selectDaysPicker.text = it.reminderDays
                }
            })

        // Set up listeners
        selectTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, hourOfDay, minuteOfHour ->
                    val selectedTime = String.format("%02d:%02d", hourOfDay, minuteOfHour)
                    timingEditText.setText(selectedTime)
                },
                hour,
                minute,
                true // 24-hour format
            )
            timePickerDialog.show()
        }

        selectDaysPicker.setOnClickListener {
            // Show a dialog for selecting days
            val daysOfWeek = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            val checkedDays = booleanArrayOf(false, false, false, false, false, false, false) // Initialize based on existing data

            // Observe the medication details and update the UI
            medicationViewModel.getMedicationById(medicationId)
                .observe(this, Observer { medication ->
                    medication?.let {
                        Log.d("EditMedication", "Loaded medication: ${it.name}")
                        supportActionBar?.title = "Edit ${it.name}"
                        nameEditText.setText(it.name)
                        dosageEditText.setText(it.dosage)
                        timingEditText.setText(it.reminderTime) // Use reminderTime
                        selectDaysPicker.text =
                            it.reminderDays ?: "" // Use Elvis operator to provide default if null
                        // Update checkedDays based on existing reminderDays
                        val existingDays =
                            it.reminderDays?.split(",")?.map { it.trim() } ?: emptyList()
                        for (i in daysOfWeek.indices) {
                            checkedDays[i] = existingDays.contains(daysOfWeek[i])
                        }
                    }
                })
            AlertDialog.Builder(this)
                .setTitle("Select Reminder Days")
                .setMultiChoiceItems(daysOfWeek, checkedDays) { _, which, isChecked ->
                    checkedDays[which] = isChecked
                }
                .setPositiveButton("OK") { _, _ ->
                    val selectedDays = mutableListOf<String>()
                    for (i in checkedDays.indices) {
                        if (checkedDays[i]) {
                            selectedDays.add(daysOfWeek[i])
                        }
                    }
                    selectDaysPicker.text = selectedDays.joinToString(",")
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()
            val reminderTime = timingEditText.text.toString().trim()
            val reminderDays = selectDaysPicker.text.toString().trim()

            if (name.isNotEmpty() && dosage.isNotEmpty() && reminderTime.isNotEmpty() && medicationId != -1 && reminderDays.isNotEmpty()) {
                val updatedMedication = Medication(
                    id = medicationId,
                    name = name,
                    dosage = dosage,
                    timing = reminderTime,
                    reminderTime = reminderTime,
                    reminderDays = reminderDays
                )
                lifecycleScope.launch {
                    medicationViewModel.updateMedication(updatedMedication)
                    // --- START OF NOTIFICATION LOGIC ---
                    Log.d("NotificationDebug", "Medication updated, now triggering notifications for ID: $medicationId, name: $name")
                    createNotificationChannel() // Ensure reminder channel is created
                    sendMedicationEditedNotification(updatedMedication)
                    scheduleMedicationReminder(updatedMedication) // Schedule/update the reminder
                    // --- END OF NOTIFICATION LOGIC ---
                    finish()
                }
            } else {
                // Handle empty fields error if needed
                Toast.makeText(this, "Please fill all fields and select reminder days.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the bottom navigation
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("navigateToHomeScreen", true)
                    })
                    true
                }
                // Add cases for other bottom navigation items as needed
                else -> false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.medication_reminder_channel_name)
            val descriptionText = getString(R.string.medication_reminder_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendMedicationEditedNotification(medication: Medication) {
        val builder = NotificationCompat.Builder(this, REMINDER_CHANNEL_ID) // Use the reminder channel
            .setSmallIcon(R.drawable.pill_capsule)
            .setContentTitle("Medication Edited")
            .setContentText("Your medication '${medication.name}' has been updated.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        Log.d("NotificationDebug", "Edit notification built for ID: ${medication.id}, name: ${medication.name}")

        try {
            with(NotificationManagerCompat.from(this)) {
                notify(EDIT_NOTIFICATION_ID_BASE + medication.id, builder.build()) // Use a different ID base
                Log.d("NotificationDebug", "Edit notification sent with ID: ${EDIT_NOTIFICATION_ID_BASE + medication.id}")
            }
        } catch (e: SecurityException) {
            Log.e("NotificationError", "Failed to send edit notification: ${e.message}")
        }
    }

    private fun scheduleMedicationReminder(medication: Medication) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
            putExtra("MEDICATION_ID", medication.id)
            putExtra("MEDICATION_NAME", medication.name)
            putExtra("MEDICATION_DOSAGE", medication.dosage)
        }

        val reminderTimeString = medication.reminderTime
        val reminderDaysString = medication.reminderDays

        if (!reminderTimeString.isNullOrEmpty()) {
            val reminderTimeParts = reminderTimeString.split(":")
            if (reminderTimeParts.size == 2) {
                try {
                    val hour = reminderTimeParts[0].toInt()
                    val minute = reminderTimeParts[1].toInt()

                    if (!reminderDaysString.isNullOrEmpty()) {
                        val reminderDaysList = reminderDaysString.split(",").map { it.trim() }
                        val daysOfWeekMap = mapOf(
                            "Monday" to Calendar.MONDAY,
                            "Tuesday" to Calendar.TUESDAY,
                            "Wednesday" to Calendar.WEDNESDAY,
                            "Thursday" to Calendar.THURSDAY,
                            "Friday" to Calendar.FRIDAY,
                            "Saturday" to Calendar.SATURDAY,
                            "Sunday" to Calendar.SUNDAY
                        )

                        // Cancel any existing alarms for this medication
                        val existingIntent = Intent(this, MedicationReminderReceiver::class.java)
                        val existingPendingIntent = PendingIntent.getBroadcast(
                            this,
                            medication.id, // Use the medication ID as the base requestCode
                            existingIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(existingPendingIntent)
                        Log.d("EditMedication", "Existing alarms for medication ID ${medication.id} cancelled.")

                        reminderDaysList.forEach { dayString ->
                            val dayOfWeek = daysOfWeekMap[dayString]
                            if (dayOfWeek != null) {
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = System.currentTimeMillis()
                                    set(Calendar.HOUR_OF_DAY, hour)
                                    set(Calendar.MINUTE, minute)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                    set(Calendar.DAY_OF_WEEK, dayOfWeek)

                                    val now = Calendar.getInstance()
                                    if (timeInMillis <= now.timeInMillis && get(Calendar.DAY_OF_WEEK) == dayOfWeek) {
                                        add(Calendar.WEEK_OF_YEAR, 1)
                                    } else if (timeInMillis <= now.timeInMillis && get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
                                        val daysUntilNextOccurrence = (dayOfWeek - get(Calendar.DAY_OF_WEEK) + 7) % 7
                                        add(Calendar.DAY_OF_YEAR, daysUntilNextOccurrence)
                                    }
                                }

                                val requestCode = (medication.id.toString() + dayOfWeek.toString()).hashCode()
                                val pendingIntent = PendingIntent.getBroadcast(
                                    this,
                                    requestCode,
                                    intent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )

                                alarmManager.setRepeating(
                                    AlarmManager.RTC_WAKEUP,
                                    calendar.timeInMillis,
                                    AlarmManager.INTERVAL_DAY * 7, // Repeat weekly
                                    pendingIntent
                                )
                                Log.d("EditMedication", "Scheduled reminder for ${medication.name} at ${medication.reminderTime} on $dayString (dayOfWeek: $dayOfWeek) with requestCode: $requestCode, trigger at: ${calendar.time}")
                            } else {
                                Log.e("EditMedication", "Invalid day of week: $dayString")
                                Toast.makeText(this, "Invalid day selected", Toast.LENGTH_SHORT).show()
                            }
                        }
                        Toast.makeText(this, "Reminder updated", Toast.LENGTH_SHORT).show()

                    } else {
                        Log.w("EditMedication", "Reminder days are not set for ${medication.name}. No day-specific alarm scheduled.")
                        Toast.makeText(this, "Reminder days not set", Toast.LENGTH_SHORT).show()
                        // You might want to schedule a daily alarm as a fallback or handle this differently
                    }

                } catch (e: NumberFormatException) {
                    Log.e("EditMedication", "Error parsing reminder time: $reminderTimeString", e)
                    Toast.makeText(this, "Invalid reminder time format", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("EditMedication", "Invalid reminder time format: $reminderTimeString")
                Toast.makeText(this, "Invalid reminder time format", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("EditMedication", "Reminder time is not set for ${medication.name}. No alarm scheduled.")
            Toast.makeText(this, "Reminder time not set", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "MEDICATION_ID"
    }
}