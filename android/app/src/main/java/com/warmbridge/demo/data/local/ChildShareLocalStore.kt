package com.warmbridge.demo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.childShareStore by preferencesDataStore("wb_child_last_share")

data class ChildLastShare(
    val url: String,
    val note: String,
    val titleHint: String,
    val sharedAtMillis: Long,
)

/** 本机记录最近一次成功分享，供孩子首页「最近分享」展示。 */
class ChildShareLocalStore(context: Context) {
    private val app = context.applicationContext

    private val keyUrl = stringPreferencesKey("url")
    private val keyNote = stringPreferencesKey("note")
    private val keyTitle = stringPreferencesKey("title_hint")
    private val keyAt = longPreferencesKey("shared_at")

    val lastShare: Flow<ChildLastShare?> = app.childShareStore.data.map { pref ->
        val url = pref[keyUrl].orEmpty()
        if (url.isBlank()) return@map null
        ChildLastShare(
            url = url,
            note = pref[keyNote].orEmpty(),
            titleHint = pref[keyTitle].orEmpty(),
            sharedAtMillis = pref[keyAt] ?: 0L,
        )
    }

    suspend fun saveShare(url: String, note: String, titleHint: String) {
        app.childShareStore.edit {
            it[keyUrl] = url
            it[keyNote] = note
            it[keyTitle] = titleHint
            it[keyAt] = System.currentTimeMillis()
        }
    }
}
