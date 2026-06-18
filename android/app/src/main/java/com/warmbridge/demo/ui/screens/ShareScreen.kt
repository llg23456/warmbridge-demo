package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.warmbridge.demo.R
import com.warmbridge.demo.data.local.ChildShareLocalStore
import com.warmbridge.demo.data.local.SharePrefillHolder
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.ShareRequest
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.components.WarmTopBarNavigation
import com.warmbridge.demo.ui.components.warmTextFieldColors
import com.warmbridge.demo.util.ShareUrlExtractor
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

@Composable
fun ShareScreen(
    onDone: () -> Unit,
    initialUrl: String = "",
    initialNote: String = "",
) {
    val context = LocalContext.current
    val shareStore = remember { ChildShareLocalStore(context) }
    var url by remember {
        mutableStateOf(
            initialUrl.ifBlank { "https://www.news.cn/tech/20241219/fa8d539d4b164cc190738d2943ca080c/c.html" },
        )
    }
    var rawPasteContext by remember { mutableStateOf("") }
    var note by remember { mutableStateOf(initialNote) }
    var busy by remember { mutableStateOf(false) }
    var sentOk by remember { mutableStateOf(false) }
    var errMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SharePrefillHolder.consume()?.let { (prefillUrl, prefillNote) ->
            if (prefillUrl.isNotBlank()) url = prefillUrl
            if (prefillNote.isNotBlank()) note = prefillNote
        }
    }

    val primaryLabel = when {
        busy -> stringResource(R.string.share_sending)
        sentOk -> stringResource(R.string.share_sent)
        else -> stringResource(R.string.share_send)
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.home_share_to_parents),
        onNavigate = onDone,
        navigation = WarmTopBarNavigation.Close,
        navigationEnabled = !busy,
        intro = stringResource(R.string.share_intro),
        primaryLabel = primaryLabel,
        onPrimaryClick = {
            if (!busy && !sentOk && url.isNotBlank()) {
                scope.launch {
                    busy = true
                    errMsg = null
                    try {
                        val cleanUrl = ShareUrlExtractor.normalizePaste(url)
                        val r = NetworkModule.api.share(
                            ShareRequest(
                                url = cleanUrl,
                                note = note.trim(),
                                rawPaste = rawPasteContext.ifBlank { url.trim() },
                            ),
                        )
                        if (r.ok) {
                            sentOk = true
                            val titleHint = note.trim().ifBlank { url.trim() }
                            shareStore.saveShare(
                                url = cleanUrl,
                                note = note.trim(),
                                titleHint = titleHint,
                            )
                        } else {
                            errMsg = context.getString(R.string.share_fail)
                        }
                    } catch (e: Exception) {
                        errMsg = humanizeNetworkError(e) ?: context.getString(R.string.share_fail)
                    } finally {
                        busy = false
                    }
                }
            }
        },
        primaryEnabled = !busy && !sentOk && url.isNotBlank(),
        statusContent = {
            if (sentOk) {
                WarmStatusBanner(
                    message = stringResource(R.string.share_success_banner),
                    type = WarmStatusBannerType.Success,
                )
                Spacer(Modifier.height(12.dp))
            }
            errMsg?.let { message ->
                WarmStatusBanner(message = message, type = WarmStatusBannerType.Error)
                Spacer(Modifier.height(12.dp))
            }
        },
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = { newVal ->
                if (newVal.isBlank()) {
                    rawPasteContext = ""
                    url = ""
                    return@OutlinedTextField
                }
                val normalized = ShareUrlExtractor.normalizePaste(newVal)
                val extracted = ShareUrlExtractor.extractPreferredUrl(newVal)
                val hasRichPaste =
                    (extracted != null && newVal.trim().length > extracted.length + 2) ||
                        newVal.contains("复制打开") || newVal.contains("【") || newVal.contains('#') ||
                        newVal.contains("抖音") || newVal.contains("哔哩") || newVal.contains("b23.tv")
                if (hasRichPaste) {
                    rawPasteContext = newVal.trim()
                }
                url = normalized
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.share_url_label)) },
            placeholder = { Text(stringResource(R.string.share_url_placeholder)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = warmTextFieldColors(),
            minLines = 2,
            maxLines = 5,
            enabled = !busy && !sentOk,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.share_note_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.share_note_label)) },
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = warmTextFieldColors(),
            minLines = 4,
            enabled = !busy && !sentOk,
        )
    }
}
