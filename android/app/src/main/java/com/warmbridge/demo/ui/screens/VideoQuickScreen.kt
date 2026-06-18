package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.components.warmTextFieldColors
import com.warmbridge.demo.ui.theme.WbBrandOrange
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

    WarmToolScreenScaffold(
        title = stringResource(R.string.media_video_title),
        onNavigate = onBack,
        intro = stringResource(R.string.video_quick_intro),
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
        footerHint = stringResource(R.string.video_quick_result_hint),
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
        OutlinedTextField(
            value = paste,
            onValueChange = { paste = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.video_quick_paste_label)) },
            placeholder = { Text(stringResource(R.string.video_quick_paste_hint)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = warmTextFieldColors(),
            singleLine = false,
            minLines = 4,
            enabled = !loading,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = {
                val u = extractedUrl ?: return@OutlinedButton
                runCatching {
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(u))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = extractedUrl != null && !loading,
        ) {
            Text(
                stringResource(R.string.video_quick_open_link),
                color = WbBrandOrange,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}
