package com.aurviz.personalagent.tools.scheduler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.content.getSystemService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "AURA alarms",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    enableVibration(true)
                }
            )
        }
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(intent?.getStringExtra(TITLE) ?: "AURA alarm")
            .setContentText(intent?.getStringExtra(LABEL) ?: "Your alarm is due.")
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
        manager.notify(ALARM_NOTIFICATION_ID, notification)
    }

    companion object {
        const val LABEL = "alarm_label"
        const val TITLE = "alarm_title"
        private const val CHANNEL_ID = "aura_alarms_v2"
        private const val ALARM_NOTIFICATION_ID = 7001
    }
}
