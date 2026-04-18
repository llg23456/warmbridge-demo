package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.components.AssetPhoto
import com.warmbridge.demo.ui.components.WarmPrimaryButton
import com.warmbridge.demo.ui.components.WbAssetPhotos

@Composable
fun RoleSelectScreen(
    onParent: () -> Unit,
    onChild: () -> Unit,
) {
    val pageBg = Color(0xFFF7F5F2)
    Box(Modifier.fillMaxSize()) {
        AssetPhoto(
            assetPath = WbAssetPhotos.ROLE_SELECT_HERO,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            placeholderColor = pageBg,
        )
        // 自上而下：保留上图氛围，下半部叠暖米白保证文字与按钮可读
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color(0x00000000),
                            0.38f to Color(0x66F7F5F2),
                            0.62f to Color(0xCCF7F5F2),
                            1f to pageBg,
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "暖桥",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "听懂年轻人的热点，把关心说成听得懂的话",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
            Spacer(Modifier.height(40.dp))
            WarmPrimaryButton(
                onClick = onParent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("我是家长", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onChild,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    "我是孩子",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
