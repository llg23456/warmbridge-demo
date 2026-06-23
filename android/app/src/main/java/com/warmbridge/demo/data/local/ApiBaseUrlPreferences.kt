package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.warmbridge.demo.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.apiBaseUrlDataStore by preferencesDataStore("wb_api_base_url")

class ApiBaseUrlPreferences(private val context: Context) {
    private val keyOverride = stringPreferencesKey("base_url_override")

    val overrideUrl: Flow<String?> = context.apiBaseUrlDataStore.data.map { it[keyOverride] }

    suspend fun getOverride(): String? = context.apiBaseUrlDataStore.data.first()[keyOverride]

    suspend fun setOverride(url: String?) {
        context.apiBaseUrlDataStore.edit {
            if (url.isNullOrBlank()) {
                it.remove(keyOverride)
            } else {
                it[keyOverride] = normalizeBaseUrl(url)
            }
        }
    }

    suspend fun effectiveBaseUrl(): String {
        val override = getOverride()
        return effectiveBaseUrl(override, BuildConfig.API_BASE_URL)
    }

    companion object {
        fun normalizeBaseUrl(raw: String): String {
            val v = raw.trim()
            if (v.isEmpty()) return v
            return if (v.endsWith("/")) v else "$v/"
        }

        fun isValidHttpUrl(raw: String): Boolean {
            val v = raw.trim()
            return v.startsWith("http://") || v.startsWith("https://")
        }

        fun effectiveBaseUrl(override: String?, buildDefault: String = BuildConfig.API_BASE_URL): String {
            val chosen = override?.takeIf { it.isNotBlank() } ?: buildDefault
            return normalizeBaseUrl(chosen)
        }
    }
}
