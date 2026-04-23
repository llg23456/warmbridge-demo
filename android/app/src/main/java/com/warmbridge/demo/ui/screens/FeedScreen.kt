package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 带顶栏与返回的完整屏热点列表；主导航已改用 [FeedListContent] 内嵌于 Tab。
 * 保留本组件便于深链或调试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    channel: String,
    tagToken: String,
    onOpenDetail: (String) -> Unit,
    onBack: () -> Unit,
) {
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
        FeedListContent(
            channel = channel,
            tagToken = tagToken,
            onOpenDetail = onOpenDetail,
            modifier = Modifier
                .fillMaxSize()
                .padding(pad),
        )
    }
}
