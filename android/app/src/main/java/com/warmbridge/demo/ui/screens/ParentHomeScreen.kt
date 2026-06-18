package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.ui.components.ParentAnimatedSun
import com.warmbridge.demo.ui.components.WarmEmptyState
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
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.util.estimateReadingTime
import com.warmbridge.demo.util.parentHomeGreetingResId

@Composable
fun ParentHomeScreen(
    onGoToHotTab: () -> Unit,
    onReminder: () -> Unit,
    onImageExplain: () -> Unit,
    onVideoQuick: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: ParentHomeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileName = stringResource(R.string.mine_profile_name_parent)
    val childRecommendLabel = stringResource(R.string.home_child_recommend)

    WarmHomePageShell(
        modifier = modifier,
        header = {
            WarmHomeHeader(
                greeting = {
                    WarmHomeGreetingText(
                        title = stringResource(parentHomeGreetingResId(), profileName),
                        subtitle = stringResource(R.string.parent_greeting_hint),
                        titleTrailing = {
                            ParentAnimatedSun(
                                modifier = Modifier.size(28.dp),
                                animate = false,
                                tint = WbBrandOrange,
                            )
                        },
                    )
                },
                shortcuts = parentHomeShortcuts(
                    onImageExplain = onImageExplain,
                    onVideoQuick = onVideoQuick,
                    onReminder = onReminder,
                ),
            )
        },
    ) {
        when (val state = uiState) {
            ParentHomeUiState.Loading -> {
                WarmHomeGroupCard(title = stringResource(R.string.home_child_recommend)) {
                    WarmLoadingContent(
                        message = stringResource(R.string.status_loading),
                        modifier = Modifier.fillMaxWidth(),
                        minHeight = 120.dp,
                        centered = true,
                    )
                }
            }

            is ParentHomeUiState.Error -> {
                WarmHomeGroupCard(title = stringResource(R.string.home_child_recommend)) {
                    WarmRetryState(
                        title = stringResource(R.string.home_child_recommend),
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        illustrationAsset = WbAssetPhotos.ILL_ERROR_NETWORK,
                    )
                }
            }

            ParentHomeUiState.Empty -> {
                WarmHomeGroupCard(title = stringResource(R.string.home_child_recommend)) {
                    WarmEmptyState(
                        title = stringResource(R.string.feed_empty_hint),
                        message = stringResource(R.string.home_curated_empty),
                        actionLabel = stringResource(R.string.home_see_more_focus),
                        onAction = onGoToHotTab,
                    )
                }
            }

            is ParentHomeUiState.Content -> {
                ChildRecommendCard(
                    items = state.childRecommend,
                    childRecommendLabel = childRecommendLabel,
                    onViewRelated = onGoToHotTab,
                )
                CuratedContentCard(
                    items = state.curated,
                    onOpenDetail = onOpenDetail,
                    onSeeMore = onGoToHotTab,
                )
            }
        }
    }
}

@Composable
private fun ChildRecommendCard(
    items: List<FeedItemDto>,
    childRecommendLabel: String,
    onViewRelated: () -> Unit,
) {
    WarmHomeGroupCard(title = stringResource(R.string.home_child_recommend)) {
        if (items.isEmpty()) {
            WarmHomeEmptyHint(message = stringResource(R.string.home_child_recommend_empty))
        } else {
            items.forEachIndexed { index, item ->
                WarmHomeFeedRow(
                    title = item.title,
                    subtitle = item.summary,
                    meta = item.source.takeUnless { it.isBlank() || it == childRecommendLabel },
                    showDivider = index < items.lastIndex,
                    onClick = null,
                )
            }
        }
        WarmPrimaryButton(
            onClick = onViewRelated,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 14.dp),
        ) {
            Text(stringResource(R.string.home_view_related_content))
        }
    }
}

@Composable
private fun CuratedContentCard(
    items: List<FeedItemDto>,
    onOpenDetail: (String) -> Unit,
    onSeeMore: () -> Unit,
) {
    WarmHomeGroupCard(
        title = stringResource(R.string.home_curated_content),
        titleAction = WarmHomeGroupCardTitleAction.SeeMore(
            label = stringResource(R.string.home_see_more),
            onClick = onSeeMore,
        ),
    ) {
        if (items.isEmpty()) {
            WarmHomeEmptyHint(message = stringResource(R.string.home_curated_empty))
        } else {
            items.forEachIndexed { index, item ->
                WarmHomeFeedRow(
                    title = item.title,
                    subtitle = item.summary,
                    meta = buildString {
                        append(item.source)
                        val time = estimateReadingTime(item.summary)
                        if (time.isNotBlank()) {
                            append(" · ")
                            append(time)
                        }
                    }.takeIf { it.isNotBlank() },
                    onClick = { onOpenDetail(item.id) },
                    showDivider = index < items.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun parentHomeShortcuts(
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
