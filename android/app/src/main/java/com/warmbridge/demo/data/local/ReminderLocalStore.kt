package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.reminderStore by preferencesDataStore("wb_scheduled_reminders")

class ReminderLocalStore(context: Context) {
    private val app = context.applicationContext
    private val gson = Gson()
    private val keyList = stringPreferencesKey("reminders_json")
    private val listType = object : TypeToken<List<ScheduledReminder>>() {}.type

    val reminders: Flow<List<ScheduledReminder>> = app.reminderStore.data.map { pref ->
        parseList(pref[keyList])
    }

    val pendingReminders: Flow<List<ScheduledReminder>> = reminders.map { list ->
        list.filter { it.status == ScheduledReminder.STATUS_PENDING }
            .sortedBy { it.triggerAtMillis }
    }

    suspend fun addReminder(reminder: ScheduledReminder) {
        app.reminderStore.edit { pref ->
            val current = parseList(pref[keyList])
            pref[keyList] = gson.toJson((current + reminder).sortedBy { it.triggerAtMillis })
        }
    }

    suspend fun markFired(id: String) {
        app.reminderStore.edit { pref ->
            val current = parseList(pref[keyList])
            pref[keyList] = gson.toJson(
                current.map { item ->
                    if (item.id == id) item.copy(status = ScheduledReminder.STATUS_FIRED) else item
                },
            )
        }
    }

    private fun parseList(json: String?): List<ScheduledReminder> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { gson.fromJson<List<ScheduledReminder>>(json, listType) }.getOrNull()
            ?: emptyList()
    }
}
