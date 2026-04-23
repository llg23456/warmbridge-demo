package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.BuildConfig
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbMinePageBg
import com.warmbridge.demo.ui.theme.WbRippleOrange
import com.warmbridge.demo.ui.theme.WbTextMuted

@Composable
fun MineScreen(
    isParent: Boolean,
    onReminder: () -> Unit,
    onSwitchRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAbout by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }

    val displayName = if (isParent) {
        stringResource(R.string.mine_profile_name_parent)
    } else {
        stringResource(R.string.mine_profile_name_child)
    }
    val roleLine = if (isParent) {
        stringResource(R.string.mine_profile_role_parent)
    } else {
        stringResource(R.string.mine_profile_role_child)
    }
    val avatarChar = displayName.first().toString()

    Column(
        modifier
            .fillMaxSize()
            .background(WbMinePageBg)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        ) {
            Row(
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = WbRippleOrange),
                        onClick = { /* 演示：无编辑页 */ },
                    )
                    .padding(16.dp)
                    .height(120.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = WbBrandOrange.copy(alpha = 0.12f),
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = avatarChar,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = WbBrandOrange,
                        )
                    }
                }
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        text = displayName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = roleLine,
                        fontSize = 14.sp,
                        color = WbTextMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        text = stringResource(R.string.mine_profile_edit_hint),
                        fontSize = 14.sp,
                        color = WbTextMuted.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = WbTextMuted,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        MineSettingsGroupCard {
            MineRow(stringResource(R.string.mine_switch_role), onClick = onSwitchRole, showDividerBelow = true)
            MineRow(stringResource(R.string.mine_reminder), onClick = onReminder, showDividerBelow = false)
        }

        Spacer(Modifier.height(12.dp))

        MineSettingsGroupCard {
            MineRow(stringResource(R.string.mine_about), onClick = { showAbout = true }, showDividerBelow = true)
            MineRow(stringResource(R.string.mine_privacy), onClick = { showPrivacy = true }, showDividerBelow = false)
        }
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text(stringResource(R.string.mine_about)) },
            text = {
                Text(
                    "${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}\n\n${stringResource(R.string.mine_about_message)}",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text(stringResource(R.string.mine_close))
                }
            },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.mine_privacy)) },
            text = { Text(stringResource(R.string.mine_privacy_message)) },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text(stringResource(R.string.mine_close))
                }
            },
        )
    }
}

@Composable
private fun MineSettingsGroupCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun MineRow(
    title: String,
    onClick: () -> Unit,
    showDividerBelow: Boolean,
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = WbRippleOrange),
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = WbTextMuted,
            )
        }
        if (showDividerBelow) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                color = Color(0xFFE5E5EA),
                thickness = 0.5.dp,
            )
        }
    }
}
