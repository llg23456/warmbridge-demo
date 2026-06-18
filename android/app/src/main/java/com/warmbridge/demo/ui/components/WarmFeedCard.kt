package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.ui.preview.WarmBridgePreviewData
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.util.estimateReadingTime

/** 今日关注列表中的普通条目，视觉权重低于 [WarmPriorityCard]。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmFeedCard(
    item: FeedItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    readingTime: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.compactCardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = item.source,
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
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmFeedCardPreview() {
    WarmBridgeTheme {
        WarmFeedCard(
            item = WarmBridgePreviewData.sampleFeedItem,
            onClick = {},
            readingTime = estimateReadingTime(WarmBridgePreviewData.sampleFeedItem.summary),
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
