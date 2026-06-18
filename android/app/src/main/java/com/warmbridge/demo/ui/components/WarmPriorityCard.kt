package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmPriorityCard(
    title: String,
    summary: String,
    source: String,
    readingTime: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = WbDimens.priorityCardMinHeight),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!eyebrow.isNullOrBlank()) {
                Text(
                    text = eyebrow,
                    style = MaterialTheme.typography.labelMedium,
                    color = WbBrandOrange,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = source,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!readingTime.isNullOrBlank()) {
                    Text(
                        text = readingTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!primaryActionLabel.isNullOrBlank() && onPrimaryAction != null) {
                WarmPrimaryButton(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(primaryActionLabel)
                }
            }
            if (!secondaryActionLabel.isNullOrBlank() && onSecondaryAction != null) {
                OutlinedButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(WbDimens.compactCardRadius),
                ) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmPriorityCardPreview() {
    WarmBridgeTheme {
        WarmPriorityCard(
            title = "剑风传奇相关话题",
            summary = "孩子给您分享的内容，暖桥帮您用长辈能懂的话讲讲。",
            source = "B站",
            readingTime = "约 3 分钟",
            onClick = {},
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
