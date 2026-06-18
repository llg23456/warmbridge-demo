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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.PopularVideoJobDto
import com.warmbridge.demo.data.remote.PopularVideoStartRequest
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.ui.components.WarmSectionCard
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmTopAppBar
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbPageBg
import com.warmbridge.demo.ui.theme.WbScrim
import com.warmbridge.demo.util.cleanupPopularVideoCache
import com.warmbridge.demo.util.downloadPopularVideoToCache
import com.warmbridge.demo.util.humanizeNetworkError
import com.warmbridge.demo.util.saveVideoToGallery
import com.warmbridge.demo.util.shareVideoFile
import com.warmbridge.demo.util.shareTextLink
import com.warmbridge.demo.video.PopularVideoPollWorker
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
    var stepLabel by remember { mutableStateOf(context.getString(R.string.video_popular_preparing)) }
    var err by remember { mutableStateOf<String?>(null) }
    var localVideoPath by remember { mutableStateOf<String?>(null) }
    var videoLoading by remember { mutableStateOf(false) }
    var downloadBusy by remember { mutableStateOf(false) }
    var savedToGallery by remember { mutableStateOf(false) }
    var itemSource by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableIntStateOf(0) }

    val failed = job?.status == "failed"
    val interrupted = job?.status == "interrupted"
    val done = job?.status == "done"

    fun restartJob() {
        val id = jobId
        if (!id.isNullOrBlank()) {
            PopularVideoPollWorker.cancel(context, id)
        }
        jobId = null
        job = null
        err = null
        stepLabel = context.getString(R.string.video_popular_preparing)
        localVideoPath = null
        videoLoading = false
        retryTick++
    }

    DisposableEffect(jobId, done, savedToGallery) {
        onDispose {
            cleanupPopularVideoCache(context)
            val id = jobId
            if (!id.isNullOrBlank() && done && !savedToGallery) {
                scope.launch(Dispatchers.IO) {
                    runCatching { NetworkModule.api.releasePopularVideo(id) }
                        .onSuccess { Log.d(TAG, "released server mp4 jobId=$id") }
                        .onFailure { Log.w(TAG, "release server mp4 failed jobId=$id", it) }
                }
            }
            Log.d(TAG, "left screen, popular video cache cleared")
        }
    }

    LaunchedEffect(itemId) {
        runCatching { NetworkModule.api.item(itemId) }
            .onSuccess { itemSource = it.source }
    }

    LaunchedEffect(itemId, initialJobId, retryTick) {
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

    LaunchedEffect(jobId, job?.status) {
        val id = jobId ?: return@LaunchedEffect
        val st = job?.status
        if (st == "done" || st == "failed" || st == "interrupted") {
            PopularVideoPollWorker.cancel(context, id)
            return@LaunchedEffect
        }
        val t = job?.title?.takeIf { it.isNotBlank() } ?: context.getString(R.string.video_popular_title)
        PopularVideoPollWorker.schedule(context, id, itemId, t)
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
                if (st.job.status == "done" || st.job.status == "failed" || st.job.status == "interrupted") {
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
                        withContext(Dispatchers.Main) {
                            err = context.getString(R.string.video_popular_download_fail, msg)
                        }
                    }
                }
        }
        videoLoading = false
    }

    val showOverlay = (job == null || job?.status == "running" || failed || interrupted) && !videoLoading

    Scaffold(
        topBar = {
            WarmTopAppBar(
                title = stringResource(R.string.video_popular_title),
                onNavigate = onBack,
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
                    .padding(horizontal = WbDimens.screenPadding),
            ) {
                when {
                    done && !localVideoPath.isNullOrBlank() -> {
                        VideoPlayerBlock(
                            path = localVideoPath!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f),
                        )
                        Spacer(Modifier.height(WbDimens.sectionGap))
                        DoneActions(
                            sharePageUrl = job?.sharePageUrl.orEmpty(),
                            downloadBusy = downloadBusy,
                            onShareVideo = { shareVideoFile(context, localVideoPath!!) },
                            onShareLink = {
                                val link = job?.sharePageUrl?.trim().orEmpty()
                                if (link.isNotBlank()) shareTextLink(context, link)
                            },
                            onDownload = {
                                if (downloadBusy) return@DoneActions
                                scope.launch {
                                    downloadBusy = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            saveVideoToGallery(
                                                context,
                                                localVideoPath!!,
                                                job?.title ?: context.getString(R.string.video_popular_title),
                                            )
                                        }
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.video_popular_saved_toast),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        savedToGallery = true
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            context.getString(
                                                R.string.video_popular_save_fail,
                                                e.message?.take(60) ?: context.getString(R.string.status_unknown_error),
                                            ),
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
                            stringResource(R.string.video_popular_done_explain_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        WarmSectionCard(
                            title = stringResource(R.string.video_popular_explain_section),
                            modifier = Modifier.padding(top = WbDimens.sectionGap),
                        ) {
                            ExplainPanel(
                                itemId = itemId,
                                showExplainButton = true,
                                autoExplainOnLoad = false,
                                itemSource = itemSource,
                            )
                        }
                        Spacer(Modifier.height(32.dp))
                    }

                    done && videoLoading -> {
                        WarmLoadingContent(
                            message = stringResource(R.string.video_popular_loading_video),
                            modifier = Modifier.fillMaxWidth(),
                            minHeight = 160.dp,
                            centered = true,
                        )
                    }

                    !done -> {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            stringResource(R.string.video_popular_generating_intro),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (!err.isNullOrBlank() && !(done && !localVideoPath.isNullOrBlank())) {
                    Spacer(Modifier.height(12.dp))
                    WarmStatusBanner(message = err!!, type = WarmStatusBannerType.Error)
                }
            }

            if (showOverlay) {
                PopularVideoProgressOverlay(
                    progress = ((job?.progress ?: 5).coerceAtLeast(5)) / 100f,
                    stepLabel = stepLabel,
                    failed = failed,
                    interrupted = interrupted,
                    errorStep = if (failed) job?.errorStep else null,
                    errorMessage = when {
                        interrupted -> job?.errorMessage?.ifBlank { null }
                            ?: stringResource(R.string.video_popular_interrupted_banner)
                        failed -> job?.errorMessage?.ifBlank { err } ?: err
                        else -> null
                    },
                    onRetry = { restartJob() },
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
        modifier = modifier.clip(RoundedCornerShape(WbDimens.cardRadius)),
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
                                    contentDescription = stringResource(R.string.video_popular_play_cd),
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                        Text(
                            if (prepared) {
                                stringResource(R.string.video_popular_tap_play)
                            } else {
                                stringResource(R.string.video_popular_video_loading)
                            },
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
private fun DoneActions(
    sharePageUrl: String,
    downloadBusy: Boolean,
    onShareVideo: () -> Unit,
    onShareLink: () -> Unit,
    onDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WarmPrimaryButton(
            onClick = onShareVideo,
            modifier = Modifier
                .fillMaxWidth()
                .height(WbDimens.touchMin),
        ) {
            Text(
                stringResource(R.string.video_popular_share_video),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        if (sharePageUrl.isNotBlank()) {
            OutlinedButton(onClick = onShareLink, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.video_popular_share_link))
            }
        }
        OutlinedButton(
            onClick = onDownload,
            modifier = Modifier.fillMaxWidth(),
            enabled = !downloadBusy,
        ) {
            Text(
                if (downloadBusy) {
                    stringResource(R.string.video_popular_saving_gallery)
                } else {
                    stringResource(R.string.video_popular_save_gallery)
                },
            )
        }
    }
}

@Composable
private fun PopularVideoProgressOverlay(
    progress: Float,
    stepLabel: String,
    failed: Boolean,
    interrupted: Boolean,
    errorStep: String?,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    val cardTitle = when {
        failed -> stringResource(R.string.mine_popular_status_failed)
        interrupted -> stringResource(R.string.mine_popular_status_interrupted)
        else -> stringResource(R.string.mine_popular_status_running, (progress * 100).toInt())
    }
    val retryLabel = if (interrupted) {
        stringResource(R.string.video_popular_regenerate)
    } else {
        stringResource(R.string.video_popular_retry)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(WbScrim),
        contentAlignment = Alignment.Center,
    ) {
        WarmSectionCard(
            title = cardTitle,
            modifier = Modifier
                .padding(WbDimens.screenPadding)
                .fillMaxWidth(0.92f),
        ) {
            if (!failed && !interrupted) {
                WarmLoadingContent(
                    message = stepLabel,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0.05f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = WbBrandOrange,
                    trackColor = WbBrandOrange.copy(alpha = 0.2f),
                )
            }
            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                val bannerMessage = if (failed && !errorStep.isNullOrBlank()) {
                    stringResource(R.string.video_popular_failed_step, errorStep, errorMessage)
                } else {
                    errorMessage
                }
                WarmStatusBanner(
                    message = bannerMessage,
                    type = if (interrupted) WarmStatusBannerType.Warning else WarmStatusBannerType.Error,
                )
            }
            if (failed || interrupted) {
                Spacer(Modifier.height(16.dp))
                WarmPrimaryButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WbDimens.touchMin),
                ) {
                    Text(retryLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
