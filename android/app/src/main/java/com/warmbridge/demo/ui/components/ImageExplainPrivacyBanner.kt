package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens

private val PrivacyBannerBg = Color(0xFFFFE4CC)
private val ShieldIconSize = 24.dp
private val ShieldCheckSize = 13.dp

@Composable
fun ImageExplainPrivacyBanner(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(WbDimens.compactCardRadius),
        color = PrivacyBannerBg,
    ) {
        Row(
            modifier = Modifier.padding(WbDimens.contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(ShieldIconSize),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = WbBrandOrange,
                    modifier = Modifier.matchParentSize(),
                )
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = WbBrandOrange,
                    modifier = Modifier
                        .size(ShieldCheckSize)
                        .graphicsLayer {
                            scaleX = 1.15f
                            scaleY = 1.15f
                        },
                )
            }
            Text(
                text = stringResource(R.string.image_explain_privacy),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
