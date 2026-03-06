package com.flexynotes.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.flexynotes.receiver.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun scheduleReminder(noteId: Long, title: String, content: String, timeInMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Added explicit action to ensure the intent is not dropped
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.FlexyNotes.REMINDER"
            putExtra("NOTE_ID", noteId)
            putExtra("NOTE_TITLE", title)
            putExtra("NOTE_CONTENT", content)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }

            // DEBUGGING: Visual feedback to verify calculated time
            val diffMinutes = (timeInMillis - System.currentTimeMillis()) / 60000
            if (diffMinutes < 0) {
                Toast.makeText(context, "Reminder set in the past! Will trigger immediately.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Reminder set in approx. $diffMinutes minute(s).", Toast.LENGTH_SHORT).show()
            }

        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            Toast.makeText(context, "Exact alarm permission missing, falling back to inexact.", Toast.LENGTH_SHORT).show()
        }
    }

    fun cancelReminder(noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.FlexyNotes.REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            noteId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}