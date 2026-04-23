package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.AssetPhoto
import com.warmbridge.demo.ui.components.FeedLoadingShimmer
import com.warmbridge.demo.ui.components.WbAssetPhotos
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbCardTitle
import com.warmbridge.demo.ui.theme.WbRippleOrange
import com.warmbridge.demo.ui.theme.WbSourceChipBg
import com.warmbridge.demo.ui.theme.WbTextMuted
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

    Box(modifier.fillMaxSize()) {
        when {
            loading -> {
                FeedLoadingShimmer(Modifier.align(Alignment.TopCenter))
            }

            err != null -> {
                Text(
                    err!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }

            items.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AssetPhoto(
                        assetPath = WbAssetPhotos.ILL_EMPTY_FEED,
                        modifier = Modifier.size(120.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        placeholderColor = Color(0xFFF0F0F0),
                    )
                    Text(
                        text = stringResource(R.string.feed_empty_hint),
                        fontSize = 18.sp,
                        color = WbTextMuted,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 80.dp,
                    ),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.id }) { item ->
                        val interaction = remember { MutableInteractionSource() }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = interaction,
                                    indication = ripple(color = WbRippleOrange),
                                    onClick = { onOpenDetail(item.id) },
                                ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    text = item.title,
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WbCardTitle,
                                )
                                Text(
                                    text = item.summary,
                                    fontSize = 16.sp,
                                    lineHeight = 22.sp,
                                    color = WbTextMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = WbSourceChipBg,
                                    modifier = Modifier.padding(top = 10.dp),
                                ) {
                                    Text(
                                        text = "来源：${item.source}",
                                        fontSize = 12.sp,
                                        color = WbBrandOrange,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
