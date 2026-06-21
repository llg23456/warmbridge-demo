package com.warmbridge.demo.reminder

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.warmbridge.demo.data.local.ReminderLocalStore
import java.util.concurrent.TimeUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val msg = inputData.getString(KEY_MSG) ?: return Result.failure()
        val reminderId = inputData.getString(KEY_ID)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("暖桥 · 家人提醒")
            .setContentText(msg)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFY_ID, notification)

        applicationContext.sendBroadcast(
            Intent(BROADCAST_REMINDER_FIRED).apply {
                setPackage(applicationContext.packageName)
                putExtra(EXTRA_REMINDER_TEXT, msg)
            },
        )
        if (reminderId != null) {
            ReminderLocalStore(applicationContext).markFired(reminderId)
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "warmbridge_reminder"
        /** 应用内弹窗：显式广播，仅本包可收 */
        const val BROADCAST_REMINDER_FIRED = "com.warmbridge.demo.action.REMINDER_FIRED"
        const val EXTRA_REMINDER_TEXT = "extra_reminder_text"

        private const val KEY_MSG = "msg"
        private const val KEY_ID = "reminder_id"
        private const val NOTIFY_ID = 77001

        fun enqueue(context: Context, reminderId: String, message: String, delaySeconds: Long) {
            val data = Data.Builder()
                .putString(KEY_MSG, message)
                .putString(KEY_ID, reminderId)
                .build()
            val req = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .setInputData(data)
                .addTag(workTag(reminderId))
                .build()
            WorkManager.getInstance(context).enqueue(req)
        }

        private fun workTag(reminderId: String) = "warmbridge_reminder_$reminderId"
    }
}
