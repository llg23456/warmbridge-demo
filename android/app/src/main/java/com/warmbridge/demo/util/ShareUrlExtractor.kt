package com.warmbridge.demo.util

import android.util.Patterns

/**
 * 从抖音 / B 站等「整段分享口令」里抽出可在浏览器打开的链接。
 * 优先：抖音短链 → 哔哩短链/主站 → 其它第一个 http(s) 链接。
 */
object ShareUrlExtractor {

    private val endJunkChars = charArrayOf(
        '。', '，', ',', '.', '）', ')', '】', ']', '"', '\'', '!', '！', '«', '»', '；', ';',
    )

    /** 若文本中含有效 URL，返回规范化后的首选链接；否则返回 null（保留用户原输入）。 */
    fun extractPreferredUrl(text: String): String? {
        val raw = text.trim()
        if (raw.isEmpty()) return null

        val fromWebUrl = collectWebUrls(raw)
        if (fromWebUrl.isNotEmpty()) {
            return pickPreferred(fromWebUrl)
        }

        val fromLoose = collectLooseHttpUrls(raw)
        if (fromLoose.isNotEmpty()) {
            return pickPreferred(fromLoose)
        }
        return null
    }

    /**
     * 粘贴或编辑后调用：有则替换为净链接，无则原样 trim（方便用户继续改）。
     */
    fun normalizePaste(text: String): String {
        val extracted = extractPreferredUrl(text)
        return extracted ?: text.trim()
    }

    private fun collectWebUrls(text: String): List<String> {
        val m = Patterns.WEB_URL.matcher(text)
        val out = mutableListOf<String>()
        while (m.find()) {
            out.add(sanitizeUrl(m.group()))
        }
        return out.filter { it.isNotBlank() }.distinct()
    }

    /** 兜底：系统 Patterns 对极少数口令不命中时，再扫一遍常见分享域。 */
    private fun collectLooseHttpUrls(text: String): List<String> {
        val re = Regex("""https?://[^\s<>"'（【]+[^\s<>"'）】]*""", RegexOption.IGNORE_CASE)
        return re.findAll(text)
            .map { sanitizeUrl(it.value) }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun sanitizeUrl(u: String): String {
        var s = u.trim()
        while (s.isNotEmpty() && endJunkChars.contains(s.last())) {
            s = s.dropLast(1)
        }
        return s.trimEnd('.', '。', '，', ',')
    }

    private fun pickPreferred(urls: List<String>): String {
        urls.firstOrNull { it.contains("douyin.com", ignoreCase = true) }?.let { return it }
        urls.firstOrNull { it.contains("iesdouyin.com", ignoreCase = true) }?.let { return it }
        urls.firstOrNull {
            it.contains("b23.tv", ignoreCase = true) ||
                it.contains("bilibili.com", ignoreCase = true) ||
                it.contains("bilivideo.com", ignoreCase = true)
        }?.let { return it }
        return urls.first()
    }
}
