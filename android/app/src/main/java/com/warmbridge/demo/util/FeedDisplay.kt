package com.warmbridge.demo.util

/** 根据摘要长度估算阅读时间，仅用于 UI 展示，不写回后端。 */
fun estimateReadingTime(summary: String): String {
    val minutes = (summary.length / 300).coerceAtLeast(1)
    return "约 $minutes 分钟"
}
