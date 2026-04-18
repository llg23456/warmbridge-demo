package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.data.remote.ExplainRequest
import com.warmbridge.demo.data.remote.ExplainResponse
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var item by remember { mutableStateOf<FeedItemDto?>(null) }
    var explain by remember { mutableStateOf<ExplainResponse?>(null) }
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    var followUp by remember { mutableStateOf("") }

    LaunchedEffect(itemId) {
        err = null
        try {
            item = NetworkModule.api.item(itemId)
        } catch (e: Exception) {
            err = humanizeNetworkError(e)
        }
    }

    fun runExplain(question: String? = null) {
        scope.launch {
            loading = true
            err = null
            try {
                explain = NetworkModule.api.explain(
                    ExplainRequest(itemId = itemId, question = question?.ifBlank { null }),
                )
            } catch (e: Exception) {
                err = humanizeNetworkError(e)
            } finally {
                loading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情与解读", style = MaterialTheme.typography.titleLarge) },
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
            item?.let { it ->
                Text(it.title, style = MaterialTheme.typography.headlineLarge)
                Text(
                    it.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    "来源：${it.source}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Button(
                    onClick = {
                        val intent = CustomTabsIntent.Builder().build()
                        intent.launchUrl(context, Uri.parse(it.url))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                ) {
                    Text("在浏览器中打开原文 / 视频", modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            Button(
                onClick = { runExplain(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !loading && item != null,
            ) {
                Text("AI 讲给长辈听", modifier = Modifier.padding(vertical = 8.dp))
            }
            if (loading) {
                CircularProgressIndicator(Modifier.padding(16.dp))
            }
            err?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
            }

            explain?.let { e ->
                Spacer(Modifier.height(16.dp))
                SectionTitle("用长辈能懂的话")
                Text(e.plainSummary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                SectionTitle("背景小知识")
                Text(e.background, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                SectionTitle("词语小抄")
                Text(e.glossary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(e.disclaimer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }

            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = followUp,
                onValueChange = { followUp = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("追问一句（可选）") },
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 2,
            )
            Button(
                onClick = { runExplain(followUp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                enabled = !loading && item != null,
            ) {
                Text("带着追问重新解读", modifier = Modifier.padding(vertical = 8.dp))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(
        t,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}
