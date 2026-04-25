package com.warmbridge.demo.util

private val URL_REGEX = Regex("https?://[^\\s\"'<>()\\[\\]］】]+", RegexOption.IGNORE_CASE)

private val TRAIL = charArrayOf(
    ')', '）', '.', ',', '，', '。', ';', '；', ':', '：', '」', '』', '"', '\'', '］', '】', ']',
)

/** 与后端 `video_paste.extract_first_url` 规则对齐，用于客户端「跳转」按钮。 */
fun firstHttpUrl(text: String): String? {
    val m = URL_REGEX.find(text) ?: return null
    var u = m.value
    while (u.isNotEmpty() && u.last() in TRAIL) {
        u = u.dropLast(1)
    }
    return u.trim().takeIf { it.length > 10 }
}
