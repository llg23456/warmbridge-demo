package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import android.net.Uri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.util.humanizeNetworkError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    channel: String,
    tagToken: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
    var items by remember { mutableStateOf<List<FeedItemDto>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(channel, tagToken) {
        err = null
        loading = true
        try {
            val decodedTag =
                if (tagToken == "ALL" || tagToken.isBlank()) tagToken else Uri.decode(tagToken)
            val tagParam = when {
                channel != "tag" -> null
                decodedTag == "ALL" || decodedTag.isBlank() -> null
                else -> decodedTag
            }
            val ch = when (channel) {
                "child" -> "child"
                "trend" -> "trend"
                else -> null
            }
            items = NetworkModule.api.feed(tag = tagParam, channel = ch).items
        } catch (e: Exception) {
            err = humanizeNetworkError(e)
            items = emptyList()
        } finally {
            loading = false
        }
    }

    val title = when (channel) {
        "child" -> "孩子推荐"
        "trend" -> "今日年轻人话题"
        else -> "热点列表"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad),
        ) {
            when {
                loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                err != null -> {
                    Text(
                        err!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                items.isEmpty() -> {
                    Text(
                        "暂无内容",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 16.dp),
                    ) {
                        items(items, key = { it.id }) { it ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(it.id) },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(2.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(it.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        it.summary,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(top = 8.dp),
                                    )
                                    Text(
                                        "来源：${it.source}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
