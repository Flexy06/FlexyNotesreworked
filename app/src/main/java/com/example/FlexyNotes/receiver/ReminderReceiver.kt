package com.flexynotes.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.flexynotes.app.MainActivity


class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Visual feedback
        Toast.makeText(context, "FlexyNotes: Reminder Triggered!", Toast.LENGTH_LONG).show()

        val noteId = intent.getLongExtra("NOTE_ID", -1)
        val title = intent.getStringExtra("NOTE_TITLE")?.takeIf { it.isNotBlank() } ?: "Reminder"
        val content = intent.getStringExtra("NOTE_CONTENT")?.takeIf { it.isNotBlank() } ?: "You have a note to check!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminders",
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for note reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create tap intent with deep link URI
        val tapIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("flexynotes://note/$noteId"),
            context,
            MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val tapPendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val safeIcon = android.R.drawable.ic_dialog_info

        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(safeIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(tapPendingIntent) // Set the click action
            .setAutoCancel(true) // Automatically dismiss notification when clicked
            .build()

        notificationManager.notify(noteId.toInt(), notification)
    }
}