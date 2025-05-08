package com.example.medimap.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.medimap.database.AppDatabase
import com.example.medimap.MedicationViewModel
import com.example.medimap.MedicationViewModelFactory
import java.time.LocalTime
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device booted, re-scheduling alarms.")
            val database = AppDatabase.Companion.getInstance(context)
            val medicationDao = database.medicationDao()
            val viewModelFactory = MedicationViewModelFactory(medicationDao)
            val medicationViewModel =
                MedicationViewModel(medicationDao) // Need to create instance here

            medicationViewModel.allMedications.value?.forEach { medication ->
                scheduleReminder(context, medication.name, medication.id, medication.timing)
            }
        }
    }

    private fun scheduleReminder(context: Context, medicationName: String, medicationId: Int, timingStr: String) {
        try {
            val reminderTime = LocalTime.parse(timingStr)
            val now = LocalTime.now()
            val triggerTimeMillis = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, reminderTime.hour)
                set(Calendar.MINUTE, reminderTime.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                // No need to check for past time on boot, as it will be current or future
            }.timeInMillis

            val intent = Intent(context, MedicationReminderReceiver::class.java).apply {
                putExtra("medication_name", medicationName)
                putExtra("medication_id", medicationId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                medicationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMillis,
                pendingIntent
            )
            Log.d("BootReceiver", "Re-scheduled alarm for $medicationName at $reminderTime")

        } catch (e: Exception) {
            Log.e("BootReceiver", "Error re-scheduling alarm for $medicationName: ${e.message}")
        }
    }
}