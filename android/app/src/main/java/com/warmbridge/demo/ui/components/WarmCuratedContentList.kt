package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.data.remote.FeedItemDto
import com.warmbridge.demo.ui.preview.WarmBridgePreviewData
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted

data class WarmCuratedItem(
    val id: String,
    val title: String,
    val source: String,
    val readingTime: String?,
    val thumbnailPath: String? = null,
)

fun FeedItemDto.toWarmCuratedItem(readingTime: String? = null): WarmCuratedItem =
    WarmCuratedItem(
        id = id,
        title = title,
        source = source,
        readingTime = readingTime,
    )

@Composable
fun WarmCuratedContentList(
    items: List<WarmCuratedItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    maxItems: Int = 3,
) {
    val visible = items.take(maxItems.coerceAtMost(3))
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        visible.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item.id) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = item.source,
                            style = MaterialTheme.typography.labelMedium,
                            color = WbTextMuted,
                        )
                        if (!item.readingTime.isNullOrBlank()) {
                            Text(
                                text = item.readingTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = WbTextMuted,
                            )
                        }
                    }
                }
            }
            if (index < visible.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmCuratedContentListPreview() {
    WarmBridgeTheme {
        WarmCuratedContentList(
            items = WarmBridgePreviewData.curatedItems.map { it.toWarmCuratedItem("约 2 分钟") },
            onItemClick = {},
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
