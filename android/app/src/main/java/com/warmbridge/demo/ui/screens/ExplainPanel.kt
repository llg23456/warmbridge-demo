package com.warmbridge.demo.ui.screens

import android.media.MediaPlayer
import android.util.Base64
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.ExplainRequest
import com.warmbridge.demo.data.remote.ExplainResponse
import com.warmbridge.demo.data.remote.FollowUpTurn
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.data.remote.TtsRequest
import com.warmbridge.demo.ui.components.ExplainSectionDividerSpacing
import com.warmbridge.demo.ui.components.ExplainSectionSpacing
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.ui.components.WarmSectionCard
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbDivider
import com.warmbridge.demo.util.humanizeNetworkError
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val GLOSSARY_COLLAPSE_THRESHOLD = 150

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
    var explainActivated by remember(itemId) { mutableStateOf(false) }

    val ttsUnavailable = stringResource(R.string.detail_tts_unavailable)
    val followUpNoAnswer = stringResource(R.string.detail_follow_up_no_answer)

    fun runInitialExplain() {
        explainActivated = true
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
                    err = followUpNoAnswer
                }
            } catch (e: Exception) {
                humanizeNetworkError(e)?.let { err = it }
            } finally {
                followUpLoading = false
            }
        }
    }

    LaunchedEffect(itemId, itemSource, autoExplainOnLoad) {
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
                    ttsSoft = resp.message.ifBlank { ttsUnavailable }
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
                ttsSoft = ttsUnavailable
            } finally {
                ttsLoading = false
            }
        }
    }

    val buttonGap = 12.dp
    val showExplainCards = explainActivated && explain != null
    Column(modifier = modifier.fillMaxWidth()) {
        if (showExplainButton && !showExplainCards) {
            WarmPrimaryButton(
                onClick = { runInitialExplain() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = buttonGap),
                enabled = !loading && !followUpLoading,
            ) {
                Text(
                    stringResource(R.string.detail_explain_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (loading && explain == null) {
            WarmLoadingContent(
                message = stringResource(R.string.detail_explain_loading),
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        err?.let {
            Spacer(Modifier.height(8.dp))
            WarmStatusBanner(message = it, type = WarmStatusBannerType.Error)
        }

        explain?.takeIf { explainActivated }?.let { e ->
            Spacer(Modifier.height(WbDimens.sectionGap))

            WarmSectionCard(
                title = stringResource(R.string.explain_card_main_title),
                prominentTitle = true,
                showTitleDivider = true,
                compactContentSpacing = true,
                headerAction = if (!hideListen && e.plainSummary.isNotBlank()) {
                    {
                        ExplainPlaySummaryAction(
                            enabled = !loading && !followUpLoading && !ttsLoading,
                            onClick = { playTts(e.plainSummary) },
                        )
                    }
                } else {
                    null
                },
            ) {
                if (!e.fromLlm) {
                    WarmStatusBanner(
                        message = stringResource(R.string.explain_offline_demo),
                        type = WarmStatusBannerType.Info,
                    )
                    Spacer(Modifier.height(12.dp))
                }

                ExplainCollapsibleSection(
                    title = stringResource(R.string.explain_what_is_this),
                    initiallyExpanded = true,
                ) {
                    Text(e.plainSummary, style = MaterialTheme.typography.bodyLarge)
                }

                ExplainCollapsibleSection(
                    title = stringResource(R.string.explain_relevant_to_you),
                    initiallyExpanded = true,
                    showTopDivider = true,
                ) {
                    Text(
                        stringResource(R.string.detail_relevant_generic),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                ExplainCollapsibleSection(
                    title = stringResource(R.string.explain_what_to_do),
                    initiallyExpanded = true,
                    showTopDivider = true,
                ) {
                    Text(
                        stringResource(R.string.explain_view_original_or_ask),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                ttsSoft?.let { tip ->
                    Spacer(Modifier.height(12.dp))
                    WarmStatusBanner(
                        message = tip,
                        type = WarmStatusBannerType.Info,
                    )
                }
            }

            WarmSectionCard(
                title = stringResource(R.string.explain_suggested_questions),
                modifier = Modifier.padding(top = WbDimens.sectionGap),
                prominentTitle = true,
                showTitleDivider = true,
                compactContentSpacing = true,
            ) {
                if (e.suggestedQuestions.isNotEmpty()) {
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
                    Spacer(Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = followUp,
                    onValueChange = { followUp = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.detail_follow_up_field)) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    minLines = 2,
                    enabled = !followUpLoading,
                )
                WarmPrimaryButton(
                    onClick = { runFollowUp(followUp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = buttonGap),
                    enabled = !followUpLoading && followUp.isNotBlank(),
                ) {
                    Text(
                        stringResource(R.string.detail_follow_up_submit),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                if (followUpLoading) {
                    WarmLoadingContent(
                        message = stringResource(R.string.detail_loading_followup),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                if (followUpHistory.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    val collapseHistory = followUpHistory.size > 3
                    ExplainCollapsibleSection(
                        title = stringResource(R.string.detail_follow_up_history),
                        initiallyExpanded = !collapseHistory,
                    ) {
                        followUpHistory.forEachIndexed { index, turn ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                            Text(
                                stringResource(R.string.detail_follow_up_question, turn.question),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(turn.answer, style = MaterialTheme.typography.bodyLarge)
                            if (turn.searched) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    stringResource(R.string.detail_follow_up_searched),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                    }
                }
            }

            if (e.glossary.isNotBlank() || e.background.isNotBlank() || e.disclaimer.isNotBlank()) {
                WarmSectionCard(
                    title = stringResource(R.string.explain_card_more_title),
                    modifier = Modifier.padding(top = WbDimens.sectionGap),
                    prominentTitle = true,
                    showTitleDivider = true,
                    compactContentSpacing = true,
                ) {
                    if (e.glossary.isNotBlank()) {
                        ExplainCollapsibleSection(
                            title = stringResource(R.string.explain_glossary),
                            initiallyExpanded = e.glossary.length <= GLOSSARY_COLLAPSE_THRESHOLD,
                        ) {
                            Text(e.glossary, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (e.background.isNotBlank()) {
                        if (e.glossary.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = ExplainSectionDividerSpacing),
                                color = WbDivider,
                            )
                        }
                        ExplainCollapsibleSection(
                            title = stringResource(R.string.explain_background),
                            initiallyExpanded = false,
                        ) {
                            Text(e.background, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (e.disclaimer.isNotBlank()) {
                        if (e.glossary.isNotBlank() || e.background.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = ExplainSectionDividerSpacing),
                                color = WbDivider,
                            )
                        }
                        ExplainCollapsibleSection(
                            title = stringResource(R.string.explain_disclaimer),
                            initiallyExpanded = false,
                        ) {
                            Text(
                                e.disclaimer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        if (beforeFollowUp != null) {
            Column(Modifier.padding(top = buttonGap)) {
                beforeFollowUp()
            }
        }
    }
}

@Composable
private fun ExplainPlaySummaryAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayCircleOutline,
            contentDescription = stringResource(R.string.detail_play_summary),
            tint = WbBrandOrange,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.detail_play_summary),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = WbBrandOrange,
        )
    }
}

@Composable
private fun ExplainCollapsibleSection(
    title: String,
    initiallyExpanded: Boolean,
    modifier: Modifier = Modifier,
    showTopDivider: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (showTopDivider) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = ExplainSectionDividerSpacing),
            color = WbDivider,
        )
    }
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = ExplainSectionSpacing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = ExplainSectionSpacing),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = stringResource(
                    if (expanded) R.string.detail_section_collapse else R.string.detail_section_expand,
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            content()
        }
    }
}










