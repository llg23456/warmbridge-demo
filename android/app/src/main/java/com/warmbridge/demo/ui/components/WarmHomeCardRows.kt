package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbCardTitle
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted

@Composable
fun WarmHomeCardDivider(
    modifier: Modifier = Modifier,
    insetHorizontal: Dp = WbDimens.contentPadding,
) {
    androidx.compose.material3.HorizontalDivider(
        modifier = modifier.padding(horizontal = insetHorizontal),
        color = com.warmbridge.demo.ui.theme.WbDivider,
        thickness = 0.5.dp,
    )
}

/** 首页卡片内资讯行：无箭头；[onClick] 为 null 时不可点击。 */
@Composable
fun WarmHomeFeedRow(
    title: String,
    subtitle: String? = null,
    meta: String? = null,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    dividerInsetHorizontal: Dp = WbDimens.contentPadding,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = WbCardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WbTextMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            meta?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = WbTextMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        if (showDivider) {
            WarmHomeCardDivider(insetHorizontal = dividerInsetHorizontal)
        }
    }
}

@Composable
fun WarmHomeFeaturedRow(
    eyebrow: String,
    title: String,
    summary: String,
    source: String,
    readingTime: String?,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelMedium,
                color = WbBrandOrange,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = WbCardTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = WbTextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = WbTextMuted,
                )
                if (!readingTime.isNullOrBlank()) {
                    Text(
                        text = readingTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = WbTextMuted,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
            WarmPrimaryButton(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            ) {
                Text(primaryActionLabel)
            }
        }
        if (showDivider) {
            WarmHomeCardDivider()
        }
    }
}

@Composable
fun WarmHomeSeeMoreFooter(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(label)
    }
}

@Composable
fun WarmHomeEmptyHint(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = WbTextMuted,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}
