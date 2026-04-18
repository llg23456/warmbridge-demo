package com.warmbridge.demo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.warmbridge.demo.reminder.ReminderWorker

class WarmBridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                "家人提醒",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "暖桥温情提醒" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
        }
    }
}
