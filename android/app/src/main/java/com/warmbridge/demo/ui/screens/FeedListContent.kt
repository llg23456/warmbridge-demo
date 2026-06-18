package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.FeedLoadingShimmer
import com.warmbridge.demo.ui.components.WarmEmptyState
import com.warmbridge.demo.ui.components.WarmRetryState
import com.warmbridge.demo.ui.components.WarmFeedCard
import com.warmbridge.demo.ui.components.WarmPriorityCard
import com.warmbridge.demo.ui.components.WbAssetPhotos
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.util.estimateReadingTime
import com.warmbridge.demo.util.humanizeNetworkError

@Composable
fun FeedListContent(
    channel: String,
    tagToken: String,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf<List<FeedItemDto>>(emptyList()) }
    var err by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var reloadNonce by remember { mutableIntStateOf(0) }

    LaunchedEffect(channel, tagToken, reloadNonce) {
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
            err = humanizeNetworkError(e) ?: "加载失败，请检查网络与后端地址。"
            items = emptyList()
        } finally {
            loading = false
        }
    }

    Box(modifier.fillMaxSize()) {
        when {
            loading -> {
                FeedLoadingShimmer(Modifier.align(Alignment.TopCenter))
            }

            err != null -> {
                WarmRetryState(
                    title = stringResource(R.string.focus_today),
                    message = err!!,
                    onRetry = { reloadNonce++ },
                    illustrationAsset = WbAssetPhotos.ILL_ERROR_NETWORK,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = WbDimens.sectionGap),
                )
            }

            items.isEmpty() -> {
                WarmEmptyState(
                    title = stringResource(R.string.feed_empty_hint),
                    message = stringResource(R.string.focus_today_subtitle),
                    assetPaths = listOf(
                        WbAssetPhotos.ILL_EMPTY_FEED,
                        WbAssetPhotos.EMPTY_FEED_ILLUSTRATION,
                    ),
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val featured = items.first()
                val rest = items.drop(1)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(WbDimens.sectionGap),
                    contentPadding = PaddingValues(
                        start = WbDimens.screenPadding,
                        end = WbDimens.screenPadding,
                        top = 8.dp,
                        bottom = 80.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "featured_${featured.id}") {
                        WarmPriorityCard(
                            title = featured.title,
                            summary = featured.summary,
                            source = featured.source,
                            readingTime = estimateReadingTime(featured.summary),
                            onClick = { onOpenDetail(featured.id) },
                        )
                    }
                    items(rest, key = { it.id }) { item ->
                        WarmFeedCard(
                            item = item,
                            onClick = { onOpenDetail(item.id) },
                            readingTime = estimateReadingTime(item.summary),
                        )
                    }
                }
            }
        }
    }
}
