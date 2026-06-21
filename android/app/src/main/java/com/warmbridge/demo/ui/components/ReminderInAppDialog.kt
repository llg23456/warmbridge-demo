package com.warmbridge.demo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbImageExplainBg
import com.warmbridge.demo.ui.theme.WbTextPrimary

private val DialogShape = RoundedCornerShape(24.dp)
private val ButtonShape = RoundedCornerShape(28.dp)
private val HeaderHeight = 200.dp
private val HeartDividerColor = Color(0xFFE8D4C4)

@Composable
fun ReminderInAppDialog(
    message: String,
    onAck: () -> Unit,
    onReply: () -> Unit,
) {
    Dialog(onDismissRequest = onAck) {
        Surface(
            shape = DialogShape,
            color = WbImageExplainBg,
            tonalElevation = 6.dp,
            modifier = Modifier.clip(DialogShape),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(HeaderHeight),
                ) {
                    AssetPhoto(
                        assetPath = WbAssetPhotos.REMINDER_DIALOG_HEADER,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
                        placeholderColor = WbImageExplainBg,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 8.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = WbBrandOrange,
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = stringResource(R.string.mine_reminder),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = WbBrandOrange,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    ReminderDialogHeartDivider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp),
                    )
                    Text(
                        text = message.trim().ifBlank {
                            stringResource(R.string.reminder_preset_water_text)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = WbTextPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                    ) {
                        OutlinedButton(
                            onClick = onReply,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = ButtonShape,
                            border = BorderStroke(1.dp, WbBrandOrange),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = WbBrandOrange,
                                containerColor = Color.Transparent,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_dialog_reply),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Button(
                            onClick = onAck,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = WbBrandOrange,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = stringResource(R.string.reminder_dialog_ack),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

private val HeartDividerLineWidth = 36.dp

@Composable
private fun ReminderDialogHeartDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.width(HeartDividerLineWidth),
            color = HeartDividerColor,
            thickness = 1.dp,
        )
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = WbBrandOrange,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(12.dp),
        )
        HorizontalDivider(
            modifier = Modifier.width(HeartDividerLineWidth),
            color = HeartDividerColor,
            thickness = 1.dp,
        )
    }
}
