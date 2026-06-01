package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.util.decodeSessionCoverBitmap
import com.warmbridge.demo.util.humanizeNetworkError
import com.warmbridge.demo.util.sessionCoverFile
import com.warmbridge.demo.util.supportsPopularVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
    onOpenPopularVideo: (String) -> Unit,
) {
    val context = LocalContext.current
    var item by remember { mutableStateOf<FeedItemDto?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    var coverBitmap by remember(itemId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(itemId) {
        coverBitmap = withContext(Dispatchers.IO) {
            sessionCoverFile(context, itemId)?.let { f ->
                decodeSessionCoverBitmap(f.absolutePath)?.asImageBitmap()
            }
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

            err?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            val it = item
            ExplainPanel(
                itemId = itemId,
                modifier = Modifier.padding(top = 12.dp),
                showExplainButton = item != null,
                autoExplainOnLoad = true,
                itemSource = it?.source,
                beforeFollowUp = if (it != null && it.supportsPopularVideo()) {
                    {
                        Button(
                            onClick = { onOpenPopularVideo(itemId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("通俗视频生成", modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                } else {
                    null
                },
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
