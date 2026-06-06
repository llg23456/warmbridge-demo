package com.warmbridge.demo.ui.screens

import android.media.MediaPlayer
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.warmbridge.demo.data.remote.ExplainRequest
import com.warmbridge.demo.data.remote.ExplainResponse
import com.warmbridge.demo.data.remote.FollowUpTurn
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.TtsRequest
import com.warmbridge.demo.util.humanizeNetworkError
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExplainPanel(
    itemId: String,
    modifier: Modifier = Modifier,
    showExplainButton: Boolean = true,
    autoExplainOnLoad: Boolean = false,
    itemSource: String? = null,
    beforeFollowUp: @Composable (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var explain by remember(itemId) { mutableStateOf<ExplainResponse?>(null) }
    var loading by remember(itemId) { mutableStateOf(false) }
    var followUpLoading by remember(itemId) { mutableStateOf(false) }
    var err by remember(itemId) { mutableStateOf<String?>(null) }
    var followUp by remember(itemId) { mutableStateOf("") }
    val followUpHistory = remember(itemId) { mutableStateListOf<FollowUpTurn>() }
    var ttsLoading by remember(itemId) { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var autoDone by remember(itemId) { mutableStateOf(false) }
    var hideListen by remember(itemId) { mutableStateOf(false) }
    var ttsSoft by remember(itemId) { mutableStateOf<String?>(null) }

    fun runInitialExplain() {
        scope.launch {
            loading = true
            err = null
            try {
                explain = NetworkModule.api.explain(
                    ExplainRequest(itemId = itemId, question = null),
                )
                hideListen = false
                ttsSoft = null
            } catch (e: Exception) {
                humanizeNetworkError(e)?.let { err = it }
            } finally {
                loading = false
            }
        }
    }

    fun runFollowUp(question: String) {
        val q = question.trim()
        if (q.isEmpty()) return
        scope.launch {
            followUpLoading = true
            err = null
            try {
                val resp = NetworkModule.api.explain(
                    ExplainRequest(itemId = itemId, question = q),
                )
                if (explain == null) {
                    explain = resp
                }
                val answer = resp.followUpAnswer.trim()
                if (answer.isNotEmpty()) {
                    followUpHistory.add(
                        FollowUpTurn(
                            question = q,
                            answer = answer,
                            fromLlm = resp.followUpFromLlm,
                            searched = resp.followUpSearched,
                        ),
                    )
                    followUp = ""
                } else {
                    err = "追问未返回回答，请稍后再试。"
                }
            } catch (e: Exception) {
                humanizeNetworkError(e)?.let { err = it }
            } finally {
                followUpLoading = false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(itemId, itemSource, autoExplainOnLoad) {
        if (!autoExplainOnLoad || autoDone) return@LaunchedEffect
        val src = itemSource ?: return@LaunchedEffect
        if (src == "识图" || src == "快解析") {
            autoDone = true
            loading = true
            err = null
            try {
                explain = NetworkModule.api.explain(
                    ExplainRequest(itemId = itemId, question = null),
                )
                hideListen = false
                ttsSoft = null
            } catch (e: Exception) {
                humanizeNetworkError(e)?.let { err = it }
            } finally {
                loading = false
            }
        }
    }

    fun playTts(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        scope.launch {
            ttsLoading = true
            try {
                val resp = withContext(Dispatchers.IO) {
                    NetworkModule.api.tts(TtsRequest(text = t))
                }
                if (!resp.ok || resp.audioBase64.isNullOrBlank()) {
                    hideListen = true
                    ttsSoft = resp.message.ifBlank { "语音暂不可用，请阅读文字版解读。" }
                    return@launch
                }
                val bytes = Base64.decode(resp.audioBase64, Base64.DEFAULT)
                val f = File(context.cacheDir, "wb_tts_${System.currentTimeMillis()}.wav")
                f.writeBytes(bytes)
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(f.absolutePath)
                    prepare()
                    start()
                }
            } catch (_: Exception) {
                hideListen = true
                ttsSoft = "语音暂不可用，请阅读文字版解读。"
            } finally {
                ttsLoading = false
            }
        }
    }

    val buttonGap = 12.dp
    Column(modifier = modifier.fillMaxWidth()) {
        if (showExplainButton) {
            Button(
                onClick = { runInitialExplain() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = buttonGap),
                enabled = !loading && !followUpLoading,
            ) {
                Text("AI 讲给长辈听", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        if (loading && explain == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text(
                    "正在生成解读…",
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
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
            if (e.suggestedQuestions.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionTitle("随口追问")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    e.suggestedQuestions.forEach { q ->
                        AssistChip(
                            onClick = { runFollowUp(q) },
                            label = { Text(q, style = MaterialTheme.typography.bodyLarge) },
                            enabled = !followUpLoading,
                        )
                    }
                }
            }
            if (!hideListen && e.plainSummary.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { playTts(e.plainSummary) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !loading && !followUpLoading && !ttsLoading,
                ) {
                    Text("听这段摘要", modifier = Modifier.padding(vertical = 8.dp))
                }
            }
            ttsSoft?.let { tip ->
                Spacer(Modifier.height(8.dp))
                Text(tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.tertiary)
            }
        }
        if (beforeFollowUp != null) {
            Column(Modifier.padding(top = buttonGap)) {
                beforeFollowUp()
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = followUp,
            onValueChange = { followUp = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("追问一句（可选）") },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 2,
            enabled = !followUpLoading,
        )
        Button(
            onClick = { runFollowUp(followUp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = buttonGap),
            enabled = !followUpLoading && followUp.isNotBlank(),
        ) {
            Text("带着追问重新解读", modifier = Modifier.padding(vertical = 8.dp))
        }
        if (followUpLoading) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(Modifier.size(28.dp))
                Text(
                    stringResource(R.string.detail_loading_followup),
                    modifier = Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (followUpHistory.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("追问记录")
            followUpHistory.forEach { turn ->
                Spacer(Modifier.height(10.dp))
                Text(
                    "问：${turn.question}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(turn.answer, style = MaterialTheme.typography.bodyLarge)
                if (turn.searched) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "（已联网补充）",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
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
