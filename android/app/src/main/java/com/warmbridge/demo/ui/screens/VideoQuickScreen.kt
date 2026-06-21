package com.warmbridge.demo.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.VideoQuickRequest
import com.warmbridge.demo.ui.components.VideoQuickHeroSection
import com.warmbridge.demo.ui.components.VideoQuickParseHintBanner
import com.warmbridge.demo.ui.components.VideoQuickPasteCard
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbImageExplainBg
import com.warmbridge.demo.ui.theme.WbSurface
import com.warmbridge.demo.util.firstHttpUrl
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch

@Composable
fun VideoQuickScreen(
    onBack: () -> Unit,
    onDoneToDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var paste by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    val extractedUrl = remember(paste) { firstHttpUrl(paste) }

    val primaryLabel = if (loading) {
        stringResource(R.string.video_quick_parsing)
    } else {
        stringResource(R.string.video_quick_parse)
    }

    val pasteFromClipboard: () -> Unit = {
        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val text = clip?.primaryClip?.getItemAt(0)?.text?.toString()
        if (!text.isNullOrBlank()) {
            paste = text
        }
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.media_video_title),
        onNavigate = onBack,
        primaryLabel = primaryLabel,
        onPrimaryClick = {
            scope.launch {
                loading = true
                err = null
                try {
                    val resp = NetworkModule.api.videoQuick(
                        VideoQuickRequest(paste = paste.trim()),
                    )
                    onDoneToDetail(resp.itemId)
                } catch (e: Exception) {
                    err = humanizeNetworkError(e) ?: context.getString(R.string.video_quick_fail)
                } finally {
                    loading = false
                }
            }
        },
        primaryEnabled = !loading && extractedUrl != null,
        navigationEnabled = !loading,
        primaryButtonBottomPadding = 28.dp,
        containerColor = WbImageExplainBg,
        topBarContainerColor = WbSurface,
        headerContent = {
            VideoQuickHeroSection()
        },
        bottomBarPrefix = {
            VideoQuickParseHintBanner()
        },
        bottomBarPrefixHorizontalPadding = 12.dp,
        statusContent = {
            if (loading) {
                WarmLoadingContent(message = stringResource(R.string.video_quick_parsing))
                Spacer(Modifier.height(12.dp))
            }
            err?.let { message ->
                WarmStatusBanner(message = message, type = WarmStatusBannerType.Error)
                Spacer(Modifier.height(12.dp))
            }
        },
    ) {
        VideoQuickPasteCard(
            value = paste,
            onValueChange = { paste = it },
            onPaste = pasteFromClipboard,
            enabled = !loading,
        )
        if (extractedUrl != null) {
            TextButton(
                onClick = {
                    runCatching {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(extractedUrl))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = !loading,
            ) {
                Text(
                    stringResource(R.string.video_quick_open_link),
                    color = WbBrandOrange,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
