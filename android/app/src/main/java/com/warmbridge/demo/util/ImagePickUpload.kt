package com.warmbridge.demo.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * 从相册 [Uri] 读出真实字节流，构造与后端 `File(..., alias="file")` 一致的 multipart 字段 **file**。
 * 避免仅展示 content:// 数字 ID 却未读出流导致上传空包或异常。
 */
fun buildImageUploadPart(context: Context, uri: Uri): MultipartBody.Part {
    val cr = context.contentResolver
    val mime = cr.getType(uri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"

    var displayName: String? = null
    runCatching {
        cr.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) displayName = c.getString(idx)
            }
        }
    }

    val bytes = cr.openInputStream(uri)?.use { it.readBytes() }
        ?: runCatching {
            cr.openAssetFileDescriptor(uri, "r")?.use { afd -> afd.createInputStream().readBytes() }
        }.getOrNull()
        ?: throw IllegalStateException("无法打开所选图片，请重新从相册选择。")

    if (bytes.isEmpty()) {
        throw IllegalStateException("图片数据为空，请换一张图或检查相册权限。")
    }
    if (bytes.size < 32) {
        throw IllegalStateException("图片数据过短，可能未正确读取，请重新选择。")
    }
    val max = 8 * 1024 * 1024
    if (bytes.size > max) {
        throw IllegalStateException("图片过大（超过 8MB），请选较小的截图。")
    }

    val ext = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    val fileName = when {
        mime.contains("png") || ext == "png" -> "upload.png"
        mime.contains("webp") || ext == "webp" -> "upload.webp"
        mime.contains("gif") || ext == "gif" -> "upload.gif"
        mime.contains("heic") || ext == "heic" || ext == "heif" -> "upload.heic"
        mime.startsWith("image/") -> if (ext.isNotBlank()) "upload.$ext" else "upload.jpg"
        else -> "upload.jpg"
    }

    val mediaType = mime.toMediaTypeOrNull() ?: "application/octet-stream".toMediaTypeOrNull()
    val body = bytes.toRequestBody(mediaType)
    return MultipartBody.Part.createFormData("file", fileName, body)
}
