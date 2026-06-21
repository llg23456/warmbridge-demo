package com.warmbridge.demo.reminder

import android.content.Context
import com.warmbridge.demo.data.local.ReminderLocalStore
import com.warmbridge.demo.data.local.ScheduledReminder
import java.util.UUID

class ReminderRepository(context: Context) {
    private val app = context.applicationContext
    private val store = ReminderLocalStore(app)

    val pendingReminders = store.pendingReminders

    suspend fun schedule(message: String, delaySeconds: Long, triggerAtMillis: Long): ScheduledReminder {
        val id = UUID.randomUUID().toString()
        val reminder = ScheduledReminder(
            id = id,
            message = message,
            triggerAtMillis = triggerAtMillis,
            createdAtMillis = System.currentTimeMillis(),
        )
        ReminderWorker.enqueue(app, id, message, delaySeconds)
        store.addReminder(reminder)
        return reminder
    }
}
