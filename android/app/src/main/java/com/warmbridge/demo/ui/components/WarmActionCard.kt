package com.warmbridge.demo.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbIconTintBg
import com.warmbridge.demo.ui.theme.WbTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArrow: Boolean = true,
    iconContentDescription: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(WbDimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        interactionSource = remember { MutableInteractionSource() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = WbDimens.contentPadding),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = WbIconTintBg,
                modifier = Modifier.size(WbDimens.iconContainerSize),
            ) {
                Row(
                    Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = iconContentDescription,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(WbDimens.iconSize),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = WbTextSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (showArrow) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = WbTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
