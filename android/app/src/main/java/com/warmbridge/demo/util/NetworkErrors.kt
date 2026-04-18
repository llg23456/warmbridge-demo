package com.warmbridge.demo.util

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun humanizeNetworkError(e: Throwable): String {
    val raw = (e.message ?: e.javaClass.simpleName).trim()
    val cleartextBlocked =
        raw.contains("CLEARTEXT", ignoreCase = true) ||
            raw.contains("cleartext communication", ignoreCase = true)
    val base = when {
        cleartextBlocked -> "系统禁止对该地址使用 HTTP 明文（网络安全策略）"
        e is SocketTimeoutException -> "连接或读取超时"
        e is ConnectException -> "无法连接服务器（连接被拒绝或网络不可达）"
        e is UnknownHostException -> "找不到服务器地址（DNS/域名错误）"
        e is IOException -> "网络异常"
        else -> "请求失败"
    }
    val tips = buildString {
        appendLine("$base。")
        appendLine()
        if (cleartextBlocked) {
            appendLine("已在本 Demo 的 network_security_config 中允许 HTTP 明文；若仍出现本提示，请 Clean/Rebuild 后重装 App。")
            appendLine()
        }
        appendLine("请检查：①电脑已启动后端（server 目录执行 uvicorn，且 --host 0.0.0.0 --port 8000）；②BuildConfig.API_BASE_URL 与手机能打开的地址一致（真机在 local.properties 配 warmbridge.api.baseUrl）；③模拟器用 http://10.0.2.2:8000/；④防火墙放行 8000 端口。")
        if (raw.isNotBlank() && !raw.contains(base)) {
            appendLine()
            append("(技术信息：$raw)")
        }
    }
    return tips.trim()
}
