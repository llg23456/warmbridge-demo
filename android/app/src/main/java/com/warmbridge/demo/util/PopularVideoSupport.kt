package com.warmbridge.demo.util

import com.warmbridge.demo.data.remote.FeedItemDto
import java.net.URI

private val BILIBILI_HOSTS = setOf(
    "bilibili.com",
    "www.bilibili.com",
    "m.bilibili.com",
    "b23.tv",
)
private val DOUYIN_HOSTS = setOf(
    "douyin.com",
    "www.douyin.com",
    "v.douyin.com",
    "iesdouyin.com",
)

fun isBilibiliOrDouyinUrl(url: String): Boolean {
    val u = url.trim()
    if (!u.startsWith("http://") && !u.startsWith("https://")) return false
    val host = runCatching { URI(u).host?.lowercase() }.getOrNull() ?: return false
    return BILIBILI_HOSTS.any { host == it || host.endsWith(".$it") } ||
        DOUYIN_HOSTS.any { host == it || host.endsWith(".$it") }
}

/** 仅孩子推荐 / 快解析且为 B 站·抖音链接的条目展示「通俗视频生成」。 */
fun FeedItemDto.supportsPopularVideo(): Boolean {
    val link = url.trim()
    if (link.isBlank()) return false
    if (source != "孩子推荐" && source != "快解析") return false
    return isBilibiliOrDouyinUrl(link)
}
