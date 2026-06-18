package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.warmbridge.demo.ui.components.WarmHomeCardDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.components.WarmHomeEmptyHint
import com.warmbridge.demo.ui.components.WarmHomeFeedRow
import com.warmbridge.demo.ui.components.WarmHomeGreetingText
import com.warmbridge.demo.ui.components.WarmHomeGroupCard
import com.warmbridge.demo.ui.components.WarmHomeGroupCardTitleAction
import com.warmbridge.demo.ui.components.WarmHomeHeader
import com.warmbridge.demo.ui.components.WarmHomePageShell
import com.warmbridge.demo.ui.components.WarmHomeShortcut
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.ui.components.WarmRetryState
import com.warmbridge.demo.ui.components.WbAssetPhotos
import com.warmbridge.demo.ui.theme.WbTextMuted

@Composable
fun ChildHomeScreen(
    onShare: () -> Unit,
    onShareRecommend: (url: String, note: String) -> Unit,
    onReminder: () -> Unit,
    onImageExplain: () -> Unit,
    onVideoQuick: () -> Unit,
    onGoToHotTab: () -> Unit,
    viewModel: ChildHomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileName = stringResource(R.string.mine_profile_name_child)

    WarmHomePageShell(
        modifier = modifier,
        header = {
            WarmHomeHeader(
                greeting = {
                    WarmHomeGreetingText(
                        title = stringResource(R.string.home_greeting_child, profileName),
                        subtitle = stringResource(R.string.home_greeting_child_sub),
                    )
                },
                shortcuts = childHomeShortcuts(
                    onImageExplain = onImageExplain,
                    onVideoQuick = onVideoQuick,
                    onReminder = onReminder,
                ),
            )
        },
    ) {
        when (val state = uiState) {
            ChildHomeUiState.Loading -> {
                WarmHomeGroupCard(title = stringResource(R.string.home_recent_share)) {
                    WarmLoadingContent(
                        message = stringResource(R.string.status_loading),
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 120.dp,
                        centered = true,
                    )
                }
            }

            is ChildHomeUiState.Error -> {
                WarmHomeGroupCard(title = stringResource(R.string.home_recent_share)) {
                    WarmRetryState(
                        title = stringResource(R.string.home_recent_share),
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        illustrationAsset = WbAssetPhotos.ILL_ERROR_NETWORK,
                    )
                }
            }

            is ChildHomeUiState.Content -> {
                RecentShareCard(
                    data = state.data,
                    onShare = onShare,
                )
                RecommendToParentsCard(
                    recommendations = state.data.recommendations,
                    onShareRecommend = onShareRecommend,
                    onSeeMore = onGoToHotTab,
                )
            }
        }
    }
}

@Composable
private fun RecentShareCard(
    data: ChildHomeContent,
    onShare: () -> Unit,
) {
    WarmHomeGroupCard(title = stringResource(R.string.home_recent_share)) {
        if (data.demoFamilyLines.isNotEmpty()) {
            Text(
                text = stringResource(R.string.child_demo_status_label),
                style = MaterialTheme.typography.labelSmall,
                color = WbTextMuted,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
            data.demoFamilyLines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WbTextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        val recent = data.recentShare
        if (recent != null) {
            WarmHomeFeedRow(
                title = recent.title,
                subtitle = recent.note,
                meta = recent.timeLabel,
                showDivider = false,
                onClick = null,
            )
        } else {
            WarmHomeEmptyHint(message = stringResource(R.string.child_recent_share_empty))
        }

        WarmPrimaryButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(stringResource(R.string.home_share_to_parents))
        }
    }
}

@Composable
private fun RecommendToParentsCard(
    recommendations: List<ChildRecommendItem>,
    onShareRecommend: (url: String, note: String) -> Unit,
    onSeeMore: () -> Unit,
) {
    WarmHomeGroupCard(
        title = stringResource(R.string.home_recommend_to_parents),
        titleAction = WarmHomeGroupCardTitleAction.SeeMore(
            label = stringResource(R.string.home_see_more),
            onClick = onSeeMore,
        ),
    ) {
        if (recommendations.isEmpty()) {
            WarmHomeEmptyHint(message = stringResource(R.string.home_recommend_empty))
        } else {
            recommendations.forEachIndexed { index, item ->
                RecommendShareRow(
                    item = item,
                    onShare = {
                        val note = item.summary.ifBlank { "推荐：${item.title}" }
                        onShareRecommend(item.url, note)
                    },
                    showDivider = index < recommendations.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun RecommendShareRow(
    item: ChildRecommendItem,
    onShare: () -> Unit,
    showDivider: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 13.dp),
        )
        Text(
            text = item.source,
            style = MaterialTheme.typography.labelMedium,
            color = WbTextMuted,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        )
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.child_recommend_share))
        }
        if (showDivider) {
            WarmHomeCardDivider()
        }
    }
}

@Composable
private fun childHomeShortcuts(
    onImageExplain: () -> Unit,
    onVideoQuick: () -> Unit,
    onReminder: () -> Unit,
): List<WarmHomeShortcut> = listOf(
    WarmHomeShortcut(
        icon = Icons.Outlined.Image,
        label = stringResource(R.string.media_image_title),
        contentDescription = stringResource(R.string.media_image_title),
        onClick = onImageExplain,
    ),
    WarmHomeShortcut(
        icon = Icons.Outlined.PlayCircleOutline,
        label = stringResource(R.string.media_video_title),
        contentDescription = stringResource(R.string.media_video_title),
        onClick = onVideoQuick,
    ),
    WarmHomeShortcut(
        icon = Icons.Outlined.Alarm,
        label = stringResource(R.string.mine_reminder),
        contentDescription = stringResource(R.string.mine_reminder),
        onClick = onReminder,
    ),
)
