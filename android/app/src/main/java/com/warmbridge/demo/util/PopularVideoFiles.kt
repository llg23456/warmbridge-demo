package com.warmbridge.demo.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.data.remote.NetworkModule
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

/** 离开通俗视频页或保存到相册后调用，避免 cache 里堆满 mp4。 */
fun cleanupPopularVideoCache(context: Context, keepPath: String? = null) {
    val keep = keepPath?.let { File(it).absolutePath }
    context.cacheDir.listFiles()?.forEach { f ->
        if (!f.isFile) return@forEach
        if (!f.name.startsWith("wb_popular_") || !f.name.endsWith(".mp4")) return@forEach
        if (f.absolutePath == keep) return@forEach
        runCatching { f.delete() }
    }
}

suspend fun downloadPopularVideoToCache(context: Context, videoUrl: String): String =
    withContext(Dispatchers.IO) {
        cleanupPopularVideoCache(context)
        val client = NetworkModule.okHttpClient
        val req = Request.Builder().url(videoUrl).get().build()
        val resp = client.newCall(req).execute()
        if (!resp.isSuccessful) error("下载视频失败 HTTP ${resp.code}")
        val body = resp.body ?: error("空响应")
        val f = File(context.cacheDir, "wb_popular_${System.currentTimeMillis()}.mp4")
        f.outputStream().use { out -> body.byteStream().copyTo(out) }
        if (f.length() < 3000) error("视频文件过小，可能只有声音没有画面，请重新生成。")
        f.absolutePath
    }

fun shareVideoFile(context: Context, localPath: String) {
    val file = File(localPath)
    val uri = FileProvider.getUriForFile(
        context,
        "${BuildConfig.APPLICATION_ID}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享讲解视频"))
}

fun shareTextLink(context: Context, link: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "孩子分享的原视频链接：\n$link")
    }
    context.startActivity(Intent.createChooser(intent, "分享原视频链接"))
}

/** 保存到系统相册可见的 DCIM/Camera（多数手机在「相册-视频」里能看到）。 */
suspend fun saveVideoToGallery(context: Context, localPath: String, title: String): String =
    withContext(Dispatchers.IO) {
        val src = File(localPath)
        val name = "暖桥_${title.take(16).replace(Regex("[\\\\/:*?\"<>|]"), "_")}_${System.currentTimeMillis()}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DCIM}/Camera",
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("无法写入相册")
            resolver.openOutputStream(uri)?.use { out ->
                src.inputStream().copyTo(out)
            } ?: error("无法打开输出流")
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            cleanupPopularVideoCache(context)
            uri.toString()
        } else {
            @Suppress("DEPRECATION")
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                "Camera",
            )
            dir.mkdirs()
            val outFile = File(dir, name)
            src.copyTo(outFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(outFile.absolutePath),
                arrayOf("video/mp4"),
                null,
            )
            cleanupPopularVideoCache(context, keepPath = null)
            outFile.absolutePath
        }
    }
