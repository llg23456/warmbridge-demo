package com.warmbridge.demo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.reminder.ReminderWorker
import com.warmbridge.demo.video.PopularVideoPollWorker

class WarmBridgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NetworkModule.init(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                ReminderWorker.CHANNEL_ID,
                "家人提醒",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "暖桥温情提醒" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(ch)
            val videoCh = NotificationChannel(
                PopularVideoPollWorker.CHANNEL_ID,
                "通俗视频生成",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "通俗视频生成完成提醒" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(videoCh)
        }
    }
}
