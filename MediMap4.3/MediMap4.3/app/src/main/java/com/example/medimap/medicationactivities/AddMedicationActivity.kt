package com.example.medimap.medicationactivities

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.AlarmManagerCompat
import androidx.lifecycle.ViewModelProvider
import com.example.medimap.MainActivity
import com.example.medimap.MedicationViewModel
import com.example.medimap.MedicationViewModelFactory
import com.example.medimap.R
import com.example.medimap.database.AppDatabase
import com.example.medimap.database.Medication
import com.example.medimap.receiver.MedicationReminderReceiver
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AddMedicationActivity : AppCompatActivity() {

    private lateinit var nameEditText: TextInputEditText
    private lateinit var dosageEditText: TextInputEditText
    private lateinit var timingEditText: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var selectTimeButton: Button
    private lateinit var medicationViewModel: MedicationViewModel
    private lateinit var alarmManager: AlarmManager

    private lateinit var selectDaysPickerTextView: TextView
    private val selectedDaysOfWeek = mutableListOf<Int>()
    private val daysOfWeekArray = arrayOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private var selectedDaysBooleanArray = booleanArrayOf(false, false, false, false, false, false, false)

    // ✅ Correct Calendar mapping
    private val calendarDayOfWeekMap = listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY
    )

    private var selectedHour = -1
    private var selectedMinute = -1

    private val requestExactAlarmPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Re-attempt scheduling if needed.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medication)

        nameEditText = findViewById(R.id.medication_name_edit_text)
        dosageEditText = findViewById(R.id.medication_dosage_edit_text)
        timingEditText = findViewById(R.id.medication_timing_edit_text)
        saveButton = findViewById(R.id.save_medication_button)
        selectTimeButton = findViewById(R.id.select_time_button)
        selectDaysPickerTextView = findViewById(R.id.select_days_picker)

        alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val database = AppDatabase.getInstance(applicationContext)
        val medicationDao = database.medicationDao()
        val viewModelFactory = MedicationViewModelFactory(medicationDao)
        medicationViewModel = ViewModelProvider(this, viewModelFactory).get(MedicationViewModel::class.java)

        selectTimeButton.setOnClickListener {
            showTimePickerDialog()
        }

        selectDaysPickerTextView.setOnClickListener {
            showDaysOfWeekPickerDialog()
        }

        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val dosage = dosageEditText.text.toString().trim()

            if (name.isNotEmpty() && dosage.isNotEmpty() && selectedHour != -1 && selectedMinute != -1 && selectedDaysBooleanArray.any { it }) {
                val selectedTime = "${String.format("%02d", selectedHour)}:${String.format("%02d", selectedMinute)}"
                val selectedDaysString = daysOfWeekArray.filterIndexed { index, _ -> selectedDaysBooleanArray[index] }.joinToString(",")

                val newMedication = Medication(
                    name = name,
                    dosage = dosage,
                    timing = selectedTime,
                    reminderTime = selectedTime,
                    reminderDays = selectedDaysString
                )

                CoroutineScope(Dispatchers.IO).launch {
                    val medicationId = medicationViewModel.addMedication(newMedication)

                    val daysToSchedule = mutableListOf<Int>()
                    for (i in selectedDaysBooleanArray.indices) {
                        if (selectedDaysBooleanArray[i]) {
                            daysToSchedule.add(calendarDayOfWeekMap[i]) // ✅ corrected mapping
                        }
                    }

                    scheduleRepeatingReminders(name, medicationId, selectedHour, selectedMinute, daysToSchedule, dosage)
                    finish()
                }
            } else {
                Log.e("AddMedication", "Please fill in all fields and select a time and day.")
            }
        }

        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("navigateToHomeScreen", true)
                    })
                    true
                }
                else -> false
            }
        }

        if (!AlarmManagerCompat.canScheduleExactAlarms(alarmManager)) {
            requestExactAlarmPermission()
        }
    }

    private fun showTimePickerDialog() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                timingEditText.setText(String.format("%02d:%02d", hourOfDay, minute))
            },
            currentHour,
            currentMinute,
            true
        ).show()
    }

    private fun showDaysOfWeekPickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select Days")
            .setMultiChoiceItems(daysOfWeekArray, selectedDaysBooleanArray) { _, which, isChecked ->
                selectedDaysBooleanArray[which] = isChecked
            }
            .setPositiveButton("OK") { dialog, _ ->
                selectedDaysOfWeek.clear()
                val selectedDaysText = StringBuilder()
                for (i in selectedDaysBooleanArray.indices) {
                    if (selectedDaysBooleanArray[i]) {
                        selectedDaysOfWeek.add(calendarDayOfWeekMap[i]) // ✅ corrected mapping
                        selectedDaysText.append(daysOfWeekArray[i]).append(", ")
                        Log.d("AddMedication", "Mapped ${daysOfWeekArray[i]} to Calendar constant: ${calendarDayOfWeekMap[i]}")
                    }
                }
                selectDaysPickerTextView.text =
                    if (selectedDaysText.isNotEmpty()) selectedDaysText.removeSuffix(", ").toString() else "Select Days"
                Log.d("AddMedication", "Selected days: $selectedDaysOfWeek")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            .create()
            .show()
    }

    fun scheduleRepeatingReminders(medicationName: String, medicationId: Long, hour: Int, minute: Int, daysOfWeek: List<Int>, dosage: String) {
        daysOfWeek.forEach { day ->
            val intent = Intent(this, MedicationReminderReceiver::class.java).apply {
                action = "com.example.medimap.MEDICATION_REMINDER_ACTION"
                putExtra("MEDICATION_NAME", medicationName)
                putExtra("MEDICATION_ID", medicationId.toInt())
                putExtra("MEDICATION_DOSAGE", dosage)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this,
                (medicationId.toString() + day.toString()).hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, day)

                val now = Calendar.getInstance()
                if (before(now)) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7,
                pendingIntent
            )

            Log.d("AddMedication", "Repeating reminder scheduled for $medicationName at $hour:$minute on day: $day with ID: $medicationId, dosage: $dosage, trigger at: ${calendar.time}")
        }
    }

    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManagerCompat.canScheduleExactAlarms(alarmManager)
        } else {
            true
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            requestExactAlarmPermissionLauncher.launch(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
