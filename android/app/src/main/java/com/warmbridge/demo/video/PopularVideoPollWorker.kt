package com.warmbridge.demo.video

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import kotlinx.coroutines.delay

class PopularVideoPollWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getString(KEY_JOB_ID)?.takeIf { it.isNotBlank() } ?: return Result.failure()
        val itemId = inputData.getString(KEY_ITEM_ID).orEmpty()
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "通俗视频" }

        repeat(MAX_POLLS) {
            if (isStopped) return Result.failure()
            try {
                val st = NetworkModule.api.popularVideoStatus(jobId)
                val job = st.job
                when (job.status) {
                    "done" -> {
                        showDoneNotification(title, jobId, itemId)
                        PopularVideoPollStore.clear(applicationContext, jobId)
                        return Result.success()
                    }
                    "failed", "interrupted" -> {
                        PopularVideoPollStore.clear(applicationContext, jobId)
                        return Result.success()
                    }
                }
            } catch (_: Exception) {
                // 网络抖动：继续轮询
            }
            delay(POLL_INTERVAL_MS)
        }
        return Result.success()
    }

    private fun showDoneNotification(title: String, jobId: String, itemId: String) {
        val launch = android.content.Intent(applicationContext, com.warmbridge.demo.MainActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_JOB_ID, jobId)
            putExtra(EXTRA_ITEM_ID, itemId)
        }
        val pending = android.app.PendingIntent.getActivity(
            applicationContext,
            jobId.hashCode(),
            launch,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(applicationContext.getString(R.string.popular_video_notify_title))
            .setContentText(applicationContext.getString(R.string.popular_video_notify_body, title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(jobId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "warmbridge_popular_video"
        const val EXTRA_JOB_ID = "extra_popular_video_job_id"
        const val EXTRA_ITEM_ID = "extra_popular_video_item_id"

        private const val KEY_JOB_ID = "job_id"
        private const val KEY_ITEM_ID = "item_id"
        private const val KEY_TITLE = "title"
        private const val WORK_PREFIX = "popular_video_poll_"
        private const val MAX_POLLS = 90
        private const val POLL_INTERVAL_MS = 5_000L

        fun schedule(context: Context, jobId: String, itemId: String, title: String) {
            PopularVideoPollStore.remember(context, jobId, itemId, title)
            val data = Data.Builder()
                .putString(KEY_JOB_ID, jobId)
                .putString(KEY_ITEM_ID, itemId)
                .putString(KEY_TITLE, title)
                .build()
            val req = OneTimeWorkRequestBuilder<PopularVideoPollWorker>()
                .setInputData(data)
                .addTag(WORK_PREFIX + jobId)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_PREFIX + jobId,
                ExistingWorkPolicy.REPLACE,
                req,
            )
        }

        fun cancel(context: Context, jobId: String) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_PREFIX + jobId)
            PopularVideoPollStore.clear(context, jobId)
        }
    }
}
