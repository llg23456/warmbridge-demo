package com.warmbridge.demo.video

import android.content.Context

/** 记录后台待通知的通俗视频任务（App 被杀后 Worker 仍可继续）。 */
object PopularVideoPollStore {
    private const val PREFS = "warmbridge_popular_video_poll"

    fun remember(context: Context, jobId: String, itemId: String, title: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("item_$jobId", itemId)
            .putString("title_$jobId", title)
            .putStringSet("pending", pendingIds(context) + jobId)
            .apply()
    }

    fun clear(context: Context, jobId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("item_$jobId")
            .remove("title_$jobId")
            .putStringSet("pending", pendingIds(context) - jobId)
            .apply()
    }

    private fun pendingIds(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet("pending", emptySet())
            ?.toSet()
            ?: emptySet()
}
