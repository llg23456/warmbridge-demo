package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.ShareRequest
import com.warmbridge.demo.util.ShareUrlExtractor
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(onDone: () -> Unit) {
    // 默认填一条真实可打开的链接，演示时仍可改成任意原文 URL
    var url by remember { mutableStateOf("https://www.news.cn/tech/20241219/fa8d539d4b164cc190738d2943ca080c/c.html") }
    /** 链接框里用户粘贴的整段原文（抽出 url 后仍保留，用于标题/关键词/检索） */
    var rawPasteContext by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            TopAppBar(
                title = { Text("分享给父母", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            WarmPrimaryButton(
                onClick = {
                    if (!busy && url.isNotBlank()) {
                        scope.launch {
                            busy = true
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
                                    snack.showSnackbar("已发送成功，父母可在「孩子推荐」里看到。")
                                } else {
                                    snack.showSnackbar("发送未成功，请稍后重试。")
                                }
                            } catch (e: Exception) {
                                snack.showSnackbar(humanizeNetworkError(e))
                            } finally {
                                busy = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(56.dp),
                enabled = !busy && url.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (busy) "发送中…" else "发送", style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp, bottom = 8.dp),
        ) {
            Text(
                "粘贴抖音、B 站等整段分享文案即可，会自动抽出可打开的链接；也可直接粘贴 https 开头网址。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
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
                label = { Text("链接", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) },
                placeholder = { Text("粘贴分享口令或网址") },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                colors = fieldColors(),
                minLines = 2,
                maxLines = 5,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("推荐语（可选）", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) },
                textStyle = MaterialTheme.typography.bodyLarge,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors(),
                minLines = 4,
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    unfocusedLabelColor = MaterialTheme.colorScheme.secondary,
)
