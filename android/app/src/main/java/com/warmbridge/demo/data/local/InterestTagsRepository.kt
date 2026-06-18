package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wbInterestStore by preferencesDataStore("wb_interest_tags")

/**
 * 家长 / 孩子「按兴趣」单选标签持久化；空值表示「全部」。双端共用。
 */
class InterestTagsRepository(context: Context) {
    private val app = context.applicationContext
    private val key = stringPreferencesKey("pipe_joined_sorted")

    val selectedTag: Flow<String?> = app.wbInterestStore.data.map { pref ->
        val raw = pref[key] ?: ""
        if (raw.isBlank()) null
        else raw.split('|').firstOrNull()?.takeIf { it.isNotBlank() }
    }

    suspend fun setSelectedTag(tag: String?) {
        val encoded = tag?.trim()?.takeIf { it.isNotBlank() } ?: ""
        app.wbInterestStore.edit { it[key] = encoded }
    }
}
