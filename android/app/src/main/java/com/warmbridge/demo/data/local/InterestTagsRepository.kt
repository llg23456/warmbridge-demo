package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.wbInterestStore by preferencesDataStore("wb_interest_tags")

/**
 * 家长 / 孩子「按兴趣」多选标签持久化（pipe 拼接、排序），双端共用。
 */
class InterestTagsRepository(context: Context) {
    private val app = context.applicationContext
    private val key = stringPreferencesKey("pipe_joined_sorted")

    val tags: Flow<Set<String>> = app.wbInterestStore.data.map { pref ->
        val raw = pref[key] ?: ""
        if (raw.isBlank()) emptySet()
        else raw.split('|').filter { it.isNotBlank() }.toSet()
    }

    suspend fun setTags(tags: Set<String>) {
        val encoded = tags.sorted().joinToString("|")
        app.wbInterestStore.edit { it[key] = encoded }
    }
}
