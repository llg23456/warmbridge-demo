package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextSecondary

data class WarmToolItem(
    val icon: ImageVector,
    val title: String,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
fun WarmCompactToolRow(
    items: List<WarmToolItem>,
    modifier: Modifier = Modifier,
) {
    require(items.size <= 2) { "WarmCompactToolRow supports at most 2 items" }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(WbDimens.sectionGap),
    ) {
        items.forEach { item ->
            Surface(
                onClick = item.onClick,
                modifier = Modifier
                    .weight(1f)
                    .height(WbDimens.touchMin)
                    .semantics { contentDescription = item.contentDescription },
                shape = RoundedCornerShape(WbDimens.compactCardRadius),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = WbTextSecondary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmCompactToolRowPreview() {
    WarmBridgeTheme {
        WarmCompactToolRow(
            items = listOf(
                WarmToolItem(
                    icon = Icons.Default.Image,
                    title = "图片识梗",
                    contentDescription = "图片识梗",
                    onClick = {},
                ),
                WarmToolItem(
                    icon = Icons.Default.PlayArrow,
                    title = "视频快解析",
                    contentDescription = "视频快解析",
                    onClick = {},
                ),
            ),
            modifier = Modifier.padding(WbDimens.screenPadding),
        )
    }
}
