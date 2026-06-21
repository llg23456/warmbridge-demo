package com.warmbridge.demo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbTextMuted
import com.warmbridge.demo.ui.theme.WbTextPrimary

private val LinkIconBg = Color(0xFFFFE8D6)
private val LinkIconContainerRadius = 10.dp

@Composable
fun VideoQuickPasteCard(
    value: String,
    onValueChange: (String) -> Unit,
    onPaste: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(LinkIconContainerRadius),
                    color = LinkIconBg,
                    modifier = Modifier
                        .size(36.dp)
                        .graphicsLayer { rotationZ = 90f },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = WbBrandOrange,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = 45f },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.video_quick_card_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = WbTextPrimary,
                    fontSize = 15.sp,
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(WbDimens.compactCardRadius),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp),
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.video_quick_paste_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = WbTextMuted,
                                modifier = Modifier.padding(top = 4.dp),
                                fontSize = 12.sp,
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = enabled,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = WbTextPrimary),
                            cursorBrush = SolidColor(WbBrandOrange),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onPaste,
                            enabled = enabled,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .padding(end = 4.dp),
                                    tint = WbTextMuted,
                                )
                                Text(
                                    text = stringResource(R.string.video_quick_paste_action),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = WbTextMuted,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = WbBrandOrange,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.video_quick_card_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = WbTextMuted,
                    fontSize = 10.sp,
                )
            }
        }
    }
}
