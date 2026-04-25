package com.warmbridge.demo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import kotlin.math.max

private fun safeItemFileName(itemId: String): String =
    itemId.replace(Regex("[^a-zA-Z0-9_-]"), "_")

private fun coverFile(context: Context, itemId: String): File =
    File(context.cacheDir, "wb_session_cover_${safeItemFileName(itemId)}.bin")

/** 识图上传成功后写入缓存，供详情页展示原图（服务端不存图）。 */
fun copyUriToSessionCover(context: Context, itemId: String, uri: Uri): Boolean {
    return try {
        val out = coverFile(context, itemId)
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return false
        out.length() > 0L
    } catch (_: Exception) {
        false
    }
}

fun sessionCoverFile(context: Context, itemId: String): File? {
    val f = coverFile(context, itemId)
    return if (f.exists() && f.length() > 0L) f else null
}

/** 缩小采样，避免大图解码 OOM */
fun decodeSessionCoverBitmap(absolutePath: String, maxSidePx: Int = 2048): Bitmap? {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(absolutePath, opts)
    val w = opts.outWidth
    val h = opts.outHeight
    if (w <= 0 || h <= 0) return null
    var sample = 1
    while (max(w, h) / sample > maxSidePx) sample *= 2
    return BitmapFactory.decodeFile(
        absolutePath,
        BitmapFactory.Options().apply { inSampleSize = sample },
    )
}
