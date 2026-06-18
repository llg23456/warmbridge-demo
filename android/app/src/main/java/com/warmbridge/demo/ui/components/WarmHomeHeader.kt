package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PlayCircleOutline
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WarmHomeOnHeaderSubtext
import com.warmbridge.demo.ui.theme.WarmHomeOnHeaderText
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

data class WarmHomeShortcut(
    val icon: ImageVector,
    val label: String,
    val contentDescription: String,
    val onClick: () -> Unit,
)

@Composable
fun WarmHomeHeader(
    greeting: @Composable () -> Unit,
    shortcuts: List<WarmHomeShortcut>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 50.dp, bottom = 24.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),

        ) {
            greeting()
        }
        WarmHomeShortcutRow(
            shortcuts = shortcuts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, start = 16.dp, end = 16.dp),
        )
    }
}

@Composable
fun WarmHomeGreetingText(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleTrailing: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier) {
        if (titleTrailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = WbBrandOrange,
                )
                titleTrailing()
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = WbBrandOrange,
            )
        }
        subtitle?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = WbBrandOrange,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun WarmHomeShortcutRow(
    shortcuts: List<WarmHomeShortcut>,
    modifier: Modifier = Modifier,
) {
    require(shortcuts.size == 3) { "WarmHomeShortcutRow expects exactly 3 shortcuts" }

    Surface(
        modifier = modifier.fillMaxWidth()
            .padding(top=200.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
     ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            shortcuts.forEach { shortcut ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = WbDimens.touchMin)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = shortcut.onClick,
                        )
                        .semantics { contentDescription = shortcut.contentDescription }
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = shortcut.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(WbDimens.homeShortcutIconContainer),
                    )
                    Text(
                        text = shortcut.label,
                        fontSize = 15.sp,
                        color = WbBrandOrange,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun WarmHomeHeaderPreview() {
    WarmBridgeTheme {
        WarmHomeHeader(
            greeting = {
                WarmHomeGreetingText(
                    title = "早上好，家中的长辈",
                    subtitle = "今天值得关注",
                )
            },
            shortcuts = listOf(
                WarmHomeShortcut(Icons.Outlined.Image, "图片识梗", "图片识梗") {},
                WarmHomeShortcut(Icons.Outlined.PlayCircleOutline, "视频快解析", "视频快解析") {},
                WarmHomeShortcut(Icons.Outlined.Alarm, "温情提醒", "温情提醒") {},
            ),
        )
    }
}
