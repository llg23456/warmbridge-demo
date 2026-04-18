package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("wb_role")

class RolePreferences(private val context: Context) {
    private val keyRole = stringPreferencesKey("role")

    val role: Flow<String?> = context.dataStore.data.map { it[keyRole] }

    suspend fun setRole(role: String) {
        context.dataStore.edit { it[keyRole] = role }
    }
}
