package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.VideoQuickRequest
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.util.firstHttpUrl
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("视频快解析", style = MaterialTheme.typography.titleLarge) },
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
            Text(
                stringResource(R.string.video_quick_intro),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = paste,
                onValueChange = { paste = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.video_quick_paste_label)) },
                placeholder = { Text(stringResource(R.string.video_quick_paste_hint)) },
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = false,
                minLines = 4,
            )
            Button(
                onClick = {
                    scope.launch {
                        loading = true
                        err = null
                        try {
                            val resp = NetworkModule.api.videoQuick(
                                VideoQuickRequest(paste = paste.trim()),
                            )
                            onDoneToDetail(resp.itemId)
                        } catch (e: Exception) {
                            err = humanizeNetworkError(e)
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = !loading && extractedUrl != null,
            ) {
                Text("解析并生成解读", modifier = Modifier.padding(vertical = 8.dp))
            }
            OutlinedButton(
                onClick = {
                    val u = extractedUrl ?: return@OutlinedButton
                    runCatching {
                        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(u))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = extractedUrl != null,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WbBrandOrange),
            ) {
                Text(stringResource(R.string.video_quick_open_link), modifier = Modifier.padding(vertical = 8.dp))
            }
            if (loading) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Text(
                        stringResource(R.string.video_quick_parsing),
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            err?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
