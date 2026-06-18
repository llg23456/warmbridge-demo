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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.WarmRetryState
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmTopAppBar
import com.warmbridge.demo.ui.theme.WbDimens
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
    var reloadNonce by remember { mutableIntStateOf(0) }
    var coverBitmap by remember(itemId) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(itemId) {
        coverBitmap = withContext(Dispatchers.IO) {
            sessionCoverFile(context, itemId)?.let { f ->
                decodeSessionCoverBitmap(f.absolutePath)?.asImageBitmap()
            }
        }
    }

    LaunchedEffect(itemId, reloadNonce) {
        err = null
        try {
            item = NetworkModule.api.item(itemId)
        } catch (e: Exception) {
            err = humanizeNetworkError(e)
        }
    }

    Scaffold(
        topBar = {
            WarmTopAppBar(
                title = stringResource(R.string.detail_title),
                onNavigate = onBack,
            )
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = WbDimens.screenPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            item?.let { feedItem ->
                val showImageCover = feedItem.source == "识图" && coverBitmap != null
                if (showImageCover) {
                    Image(
                        bitmap = coverBitmap!!,
                        contentDescription = stringResource(R.string.detail_image_cover_cd),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp)
                            .clip(RoundedCornerShape(WbDimens.compactCardRadius)),
                        contentScale = ContentScale.Fit,
                    )
                    Text(
                        text = stringResource(R.string.detail_source, feedItem.source),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (feedItem.source == "识图") {
                    Text(
                        text = stringResource(R.string.detail_source, feedItem.source),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Text(
                        text = stringResource(R.string.detail_image_cache_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    Text(feedItem.title, style = MaterialTheme.typography.headlineLarge)
                    Text(
                        feedItem.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = stringResource(R.string.detail_source, feedItem.source),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (feedItem.url.isNotBlank()) {
                    Button(
                        onClick = {
                            val intent = CustomTabsIntent.Builder().build()
                            intent.launchUrl(context, Uri.parse(feedItem.url))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Text(
                            stringResource(R.string.detail_open_original),
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }

            err?.let { message ->
                if (item == null) {
                    WarmRetryState(
                        message = message,
                        onRetry = { reloadNonce++ },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                } else {
                    WarmStatusBanner(
                        message = message,
                        type = WarmStatusBannerType.Error,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }

            val feedItem = item
            ExplainPanel(
                itemId = itemId,
                modifier = Modifier.padding(top = 12.dp),
                showExplainButton = item != null,
                autoExplainOnLoad = true,
                itemSource = feedItem?.source,
                beforeFollowUp = if (feedItem != null && feedItem.supportsPopularVideo()) {
                    {
                        Button(
                            onClick = { onOpenPopularVideo(itemId) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                stringResource(R.string.detail_popular_video),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
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
