package com.warmbridge.demo.data.local

/** 进入分享页前的轻量预填，消费后即清空。 */
object SharePrefillHolder {
    @Volatile
    private var url: String? = null

    @Volatile
    private var note: String? = null

    fun set(url: String, note: String = "") {
        this.url = url
        this.note = note
    }

    fun consume(): Pair<String, String>? {
        val u = url ?: return null
        val n = note.orEmpty()
        url = null
        note = null
        return u to n
    }
}
