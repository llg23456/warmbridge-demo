package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

@Composable
fun WarmLoadingContent(
    message: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    centered: Boolean = false,
    minHeight: Dp? = null,
) {
    val indicatorSize = if (compact) 24.dp else 28.dp
    val content = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(indicatorSize),
                color = WbBrandOrange,
                strokeWidth = if (compact) 2.dp else 3.dp,
            )
            Text(
                text = message,
                modifier = Modifier.padding(start = 12.dp),
                style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
            )
        }
    }

    when {
        centered -> {
            Box(
                modifier = modifier.then(
                    if (minHeight != null) Modifier.height(minHeight) else Modifier,
                ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(if (compact) 32.dp else 40.dp),
                        color = WbBrandOrange,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        minHeight != null -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(minHeight),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
        else -> {
            Box(modifier = modifier) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmLoadingContentPreview() {
    WarmBridgeTheme {
        WarmLoadingContent(
            message = "正在加载…",
            modifier = Modifier.padding(WbDimens.screenPadding),
            minHeight = 120.dp,
            centered = true,
        )
    }
}
