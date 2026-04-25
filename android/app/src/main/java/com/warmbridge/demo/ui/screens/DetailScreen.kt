package com.warmbridge.demo.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.ExplainRequest
import com.warmbridge.demo.data.remote.ExplainResponse
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.TtsRequest
import com.warmbridge.demo.util.decodeSessionCoverBitmap
import com.warmbridge.demo.util.humanizeNetworkError
import com.warmbridge.demo.util.sessionCoverFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<FeedItemDto?>(null) }
    var explain by remember { mutableStateOf<ExplainResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var followUp by remember { mutableStateOf("") }
    var ttsLoading by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var autoExplainDone by remember(itemId) { mutableStateOf(false) }
    var hideListenSummary by remember(itemId) { mutableStateOf(false) }
    var ttsSoftMessage by remember(itemId) { mutableStateOf<String?>(null) }
    var coverBitmap by remember(itemId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(itemId) {
        coverBitmap = withContext(Dispatchers.IO) {
            sessionCoverFile(context, itemId)?.let { f ->
                decodeSessionCoverBitmap(f.absolutePath)?.asImageBitmap()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
            player = null
        }
    }

    LaunchedEffect(itemId) {
        err = null
        try {
            item = NetworkModule.api.item(itemId)
        } catch (e: Exception) {
            err = humanizeNetworkError(e)
        }
    }

    fun runExplain(question: String? = null) {
        scope.launch {
            loading = true
            err = null
            try {
                explain = NetworkModule.api.explain(
                    ExplainRequest(itemId = itemId, question = question?.ifBlank { null }),
                )
                hideListenSummary = false
                ttsSoftMessage = null
            } catch (e: Exception) {
                err = humanizeNetworkError(e)
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(item?.id, item?.source) {
        val it = item ?: return@LaunchedEffect
        if (autoExplainDone) return@LaunchedEffect
        if (it.source == "识图" || it.source == "快解析") {
            autoExplainDone = true
            runExplain(null)
        }
    }

    fun playTts(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        scope.launch {
            ttsLoading = true
            err = null
            try {
                val resp = withContext(Dispatchers.IO) {
                    NetworkModule.api.tts(TtsRequest(text = t))
                }
                if (!resp.ok || resp.audioBase64.isNullOrBlank()) {
                    hideListenSummary = true
                    ttsSoftMessage = resp.message.ifBlank { "语音暂不可用，请阅读文字版解读。" }
                    return@launch
                }
                val bytes = Base64.decode(resp.audioBase64, Base64.DEFAULT)
                val f = File(context.cacheDir, "wb_tts_${System.currentTimeMillis()}.wav")
                f.writeBytes(bytes)
                withContext(Dispatchers.Main) {
                    player?.release()
                    player = MediaPlayer().apply {
                        setDataSource(f.absolutePath)
                        prepare()
                        start()
                    }
                }
            } catch (e: Exception) {
                hideListenSummary = true
                ttsSoftMessage = "语音请求失败，请阅读文字版。（${e.message?.take(80) ?: "网络异常"}）"
            } finally {
                ttsLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情与解读", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            item?.let { it ->
                val showImageCover = it.source == "识图" && coverBitmap != null
                if (showImageCover) {
                    Image(
                        bitmap = coverBitmap!!,
                        contentDescription = "上传的图片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        "来源：${it.source}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (it.source == "识图") {
                    Text(
                        "来源：识图",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        "原图仅保存在本机；若需预览截图请返回重新上传。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text(it.title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        it.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "来源：${it.source}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (it.url.isNotBlank()) {
                    Button(
                        onClick = {
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, Uri.parse(it.url))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text("在浏览器中打开原文 / 视频", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }

            Button(
                onClick = { runExplain(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !loading && item != null,
            ) {
                Text("AI 讲给长辈听", modifier = Modifier.padding(vertical = 8.dp))
            }
            if (loading && explain == null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text(
                        "正在生成解读…",
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            err?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            explain?.let { e ->
                Spacer(Modifier.height(16.dp))
                SectionTitle("用长辈能懂的话")
                Text(e.plainSummary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                SectionTitle("背景小知识")
                Text(e.background, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                SectionTitle("词语小抄")
                Text(e.glossary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(e.disclaimer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)

                if (e.suggestedQuestions.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("随口追问")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        e.suggestedQuestions.forEach { q ->
                            AssistChip(
                                onClick = {
                                    followUp = q
                                    runExplain(q)
                                },
                                label = { Text(q, style = MaterialTheme.typography.bodyLarge) },
                            )
                        }
                    }
                }
                if (!hideListenSummary && e.plainSummary.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { playTts(e.plainSummary) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !loading && !ttsLoading,
                    ) {
                        Text("听这段摘要", modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                ttsSoftMessage?.let { tip ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = followUp,
                onValueChange = { followUp = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("追问一句（可选）") },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 2,
            )
            Button(
                onClick = { runExplain(followUp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !loading && item != null,
            ) {
                Text("带着追问重新解读", modifier = Modifier.padding(vertical = 8.dp))
            }
            if (loading && explain != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text(
                        stringResource(R.string.detail_loading_followup),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
