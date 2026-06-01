package com.warmbridge.demo.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.PopularVideoJobDto
import com.warmbridge.demo.data.remote.PopularVideoStartRequest
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbPageBg
import com.warmbridge.demo.ui.theme.WbScrim
import com.warmbridge.demo.util.cleanupPopularVideoCache
import com.warmbridge.demo.util.downloadPopularVideoToCache
import com.warmbridge.demo.util.humanizeNetworkError
import com.warmbridge.demo.util.saveVideoToGallery
import com.warmbridge.demo.util.shareVideoFile
import com.warmbridge.demo.util.shareTextLink
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "WbVideoGen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPopularScreen(
    itemId: String,
    initialJobId: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var jobId by remember { mutableStateOf(initialJobId?.takeIf { it.isNotBlank() }) }
    var job by remember { mutableStateOf<PopularVideoJobDto?>(null) }
    var stepLabel by remember { mutableStateOf("准备中…") }
    var err by remember { mutableStateOf<String?>(null) }
    var localVideoPath by remember { mutableStateOf<String?>(null) }
    var videoLoading by remember { mutableStateOf(false) }
    var downloadBusy by remember { mutableStateOf(false) }
    var itemSource by remember { mutableStateOf<String?>(null) }

    val failed = job?.status == "failed"
    val done = job?.status == "done"

    DisposableEffect(Unit) {
        onDispose {
            cleanupPopularVideoCache(context)
            Log.d(TAG, "left screen, popular video cache cleared")
        }
    }

    LaunchedEffect(itemId) {
        runCatching { NetworkModule.api.item(itemId) }
            .onSuccess { itemSource = it.source }
    }

    LaunchedEffect(itemId, initialJobId) {
        if (!jobId.isNullOrBlank()) return@LaunchedEffect
        err = null
        try {
            val start = NetworkModule.api.startPopularVideo(PopularVideoStartRequest(itemId))
            jobId = start.jobId
            Log.d(TAG, "start itemId=$itemId jobId=${start.jobId} reused=${start.reused}")
        } catch (e: Exception) {
            humanizeNetworkError(e)?.let { err = it }
            Log.e(TAG, "start failed itemId=$itemId", e)
        }
    }

    LaunchedEffect(jobId) {
        val id = jobId ?: return@LaunchedEffect
        while (isActive) {
            try {
                val st = NetworkModule.api.popularVideoStatus(id)
                job = st.job
                stepLabel = st.stepLabel
                if (st.job.status == "done") {
                    err = null
                }
                Log.d(
                    TAG,
                    "poll jobId=$id step=${st.job.step} progress=${st.job.progress} status=${st.job.status}",
                )
                if (st.job.status == "done" || st.job.status == "failed") {
                    if (st.job.status == "failed") {
                        Log.e(
                            TAG,
                            "failed jobId=$id step=${st.job.errorStep} msg=${st.job.errorMessage}",
                        )
                    }
                    break
                }
            } catch (e: Exception) {
                humanizeNetworkError(e)?.let { err = it }
                Log.e(TAG, "poll failed jobId=$id", e)
                break
            }
            delay(2000)
        }
    }

    LaunchedEffect(done, job?.videoUrl) {
        if (!done) return@LaunchedEffect
        val url = job?.videoUrl?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        videoLoading = true
        err = null
        withContext(Dispatchers.IO) {
            runCatching { downloadPopularVideoToCache(context, url) }
                .onSuccess { path ->
                    localVideoPath = path
                    Log.d(TAG, "cached video path=$path size=${File(path).length()}")
                }
                .onFailure { e ->
                    Log.e(TAG, "download video fail url=$url", e)
                    humanizeNetworkError(e)?.let { msg ->
                        withContext(Dispatchers.Main) { err = "视频下载失败：$msg" }
                    }
                }
        }
        videoLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("通俗视频解读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .background(WbPageBg),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                when {
                    done && !localVideoPath.isNullOrBlank() -> {
                        VideoPlayerBlock(
                            path = localVideoPath!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        RowActions(
                            sharePageUrl = job?.sharePageUrl.orEmpty(),
                            downloadBusy = downloadBusy,
                            onShareVideo = { shareVideoFile(context, localVideoPath!!) },
                            onShareLink = {
                                val link = job?.sharePageUrl?.trim().orEmpty()
                                if (link.isNotBlank()) shareTextLink(context, link)
                            },
                            onDownload = {
                                if (downloadBusy) return@RowActions
                                scope.launch {
                                    downloadBusy = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            saveVideoToGallery(
                                                context,
                                                localVideoPath!!,
                                                job?.title ?: "通俗视频",
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            "已保存到相册（DCIM/Camera），请在相册「视频」中查看",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "保存失败：${e.message?.take(60) ?: "未知错误"}",
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        Log.e(TAG, "save to downloads failed", e)
                                    } finally {
                                        downloadBusy = false
                                    }
                                }
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "下方为文字解读与追问（与详情页相同，可边看边问）。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                        ExplainPanel(
                            itemId = itemId,
                            modifier = Modifier.padding(top = 12.dp),
                            showExplainButton = true,
                            autoExplainOnLoad = false,
                            itemSource = itemSource,
                        )
                        Spacer(Modifier.height(32.dp))
                    }

                    done && videoLoading -> {
                        Spacer(Modifier.height(48.dp))
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = WbBrandOrange)
                                Text(
                                    "正在加载视频…",
                                    modifier = Modifier.padding(top = 12.dp),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }

                    !done -> {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "正在为您生成 30～60 秒通俗讲解视频，可先返回；完成后在「我的」查看。",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                if (!err.isNullOrBlank() && !(done && !localVideoPath.isNullOrBlank())) {
                    Text(
                        err!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            val showOverlay = (job == null || job?.status == "running" || failed) && !videoLoading
            if (showOverlay) {
                LoadingOverlay(
                    progress = ((job?.progress ?: 5).coerceAtLeast(5)) / 100f,
                    stepLabel = stepLabel,
                    errorStep = if (failed) job?.errorStep else null,
                    errorMessage = when {
                        failed -> job?.errorMessage?.ifBlank { err } ?: err
                        else -> null
                    },
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerBlock(path: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val file = remember(path) { File(path) }
    val playUri = remember(path) {
        FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file,
        )
    }
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember(path) { mutableStateOf(false) }
    var prepared by remember(path) { mutableStateOf(false) }

    Surface(
        modifier = modifier.clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF2A2A2A),
    ) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        setOnPreparedListener { mp ->
                            mp.isLooping = false
                            prepared = true
                            seekTo(0)
                        }
                        setOnCompletionListener {
                            isPlaying = false
                        }
                        setOnErrorListener { _, what, extra ->
                            Log.e(TAG, "VideoView error what=$what extra=$extra path=$path")
                            isPlaying = false
                            false
                        }
                        videoView = this
                        setVideoURI(playUri)
                    }
                },
                update = { view -> videoView = view },
                modifier = Modifier.fillMaxSize(),
            )
            if (!isPlaying) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .clickable(enabled = prepared) {
                            videoView?.start()
                            isPlaying = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = WbBrandOrange.copy(alpha = 0.92f),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = "播放",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        Text(
                            if (prepared) "点击播放讲解视频" else "视频加载中…",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowActions(
    sharePageUrl: String,
    downloadBusy: Boolean,
    onShareVideo: () -> Unit,
    onShareLink: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onShareVideo, modifier = Modifier.fillMaxWidth()) {
            Text("分享讲解视频（微信 / QQ）")
        }
        if (sharePageUrl.isNotBlank()) {
            OutlinedButton(onClick = onShareLink, modifier = Modifier.fillMaxWidth()) {
                Text("分享原视频链接")
            }
        }
        OutlinedButton(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            enabled = !downloadBusy,
        ) {
            Text(if (downloadBusy) "正在保存到相册…" else "保存到相册视频")
        }
    }
}

@Composable
private fun LoadingOverlay(
    progress: Float,
    stepLabel: String,
    errorStep: String?,
    errorMessage: String?,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(WbScrim),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (errorMessage.isNullOrBlank()) {
                    CircularProgressIndicator(color = WbBrandOrange)
                }
                Spacer(Modifier.height(16.dp))
                Text(stepLabel, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = WbBrandOrange,
                    trackColor = WbBrandOrange.copy(alpha = 0.2f),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    val stepHint = if (!errorStep.isNullOrBlank()) "步骤「$errorStep」失败：" else ""
                    Text(
                        stepHint + errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
