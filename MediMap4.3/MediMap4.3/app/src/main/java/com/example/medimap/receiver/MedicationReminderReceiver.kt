package com.example.medimap.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medimap.R

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "onReceive() CALLED!")
        Log.d("ReminderReceiver", "Reminder triggered!")

        val medicationName = intent.getStringExtra("MEDICATION_NAME")
        val medicationId = intent.getIntExtra("MEDICATION_ID", -1) // Use getIntExtra to match EditMedicationActivity
        val medicationDosage = intent.getStringExtra("MEDICATION_DOSAGE") ?: "" // Retrieve dosage, default to empty string

        Log.d("ReminderReceiver", "Medication Name from Intent: $medicationName")
        Log.d("ReminderReceiver", "Medication ID from Intent: $medicationId")
        Log.d("ReminderReceiver", "Medication Dosage from Intent: $medicationDosage") // Log the dosage

        if (medicationName != null && medicationId != -1) {
            // Build and show the notification
            val builder = NotificationCompat.Builder(context, "medication_channel_id")
                .setSmallIcon(R.drawable.pill) // Replace with your notification icon
                .setContentTitle("Medication Reminder")
                .setContentText("It's time to take your $medicationName. Dosage: $medicationDosage") // Include dosage in the text
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true) // Dismiss the notification when tapped

            Log.d("ReminderReceiver", "Notification builder created for ID: $medicationId")

            with(NotificationManagerCompat.from(context)) {
                // notificationId is a unique ID for each notification that you must define
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d("ReminderReceiver", "Notification permission NOT granted")
                    return
                }
                Log.d("ReminderReceiver", "Notification permission GRANTED")
                notify(medicationId, builder.build()) // Use medicationId (Int) as notification ID
                Log.d("ReminderReceiver", "Notification SENT with ID: $medicationId")
            }
        } else {
            Log.e("ReminderReceiver", "Medication name or ID is null or invalid.")
        }
    }

    companion object {
        const val TAG = "MedicationReminderReceiver"
    }
}