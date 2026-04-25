package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.components.AssetPhoto
import com.warmbridge.demo.ui.components.InterestTagChips
import com.warmbridge.demo.ui.components.WarmHeaderGradientBackground
import com.warmbridge.demo.ui.components.WbAssetPhotos
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbSourceChipBg
import com.warmbridge.demo.ui.theme.WbTextMuted
import java.util.Calendar

@Composable
fun ParentHomeScreen(
    serverTags: List<String>,
    selectedTags: Set<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
    onGoToHotTab: () -> Unit,
    onReminder: () -> Unit,
    onImageExplain: () -> Unit = {},
    onVideoQuick: () -> Unit = {},
) {
    val infinite = rememberInfiniteTransition(label = "sunSway")
    val sunRotate by infinite.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sunRot",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            WarmHeaderGradientBackground(
                Modifier
                    .matchParentSize(),
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = WbBrandOrange,
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer { rotationZ = sunRotate },
                    )
                    Text(
                        text = parentGreeting(),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Text(
                    text = stringResource(R.string.parent_greeting_hint),
                    fontSize = 16.sp,
                    color = WbTextMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InterestTagChips(
                allTags = serverTags,
                selectedTags = selectedTags,
                onSelectedTagsChange = onSelectedTagsChange,
            )

            if (selectedTags.isEmpty()) {
                Text(
                    text = stringResource(R.string.parent_interest_summary_all),
                    fontSize = 14.sp,
                    color = WbTextMuted,
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = WbSourceChipBg,
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = WbBrandOrange,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.parent_selection_chip, selectedTags.size),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = WbBrandOrange,
                            )
                        }
                    }
                }
            }

            val hotInteraction = remember { MutableInteractionSource() }
            val reminderInteraction = remember { MutableInteractionSource() }
            val imgInteraction = remember { MutableInteractionSource() }
            val vidInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clickable(
                            interactionSource = imgInteraction,
                            indication = ripple(color = Color(0x26E07A3D)),
                            onClick = onImageExplain,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, WbBrandOrange),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.media_image_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.media_image_sub),
                            fontSize = 13.sp,
                            color = WbTextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(88.dp)
                        .clickable(
                            interactionSource = vidInteraction,
                            indication = ripple(color = Color(0x26E07A3D)),
                            onClick = onVideoQuick,
                        ),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, WbBrandOrange),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            stringResource(R.string.media_video_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(R.string.media_video_sub),
                            fontSize = 13.sp,
                            color = WbTextMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x26E07A3D),
                        spotColor = Color(0x26E07A3D),
                    )
                    .clickable(
                        interactionSource = hotInteraction,
                        indication = ripple(color = Color(0x26E07A3D)),
                        onClick = onGoToHotTab,
                    ),
                shape = RoundedCornerShape(16.dp),
                color = WbBrandOrange,
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Whatshot,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = stringResource(R.string.parent_go_hot),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable(
                        interactionSource = reminderInteraction,
                        indication = ripple(color = Color(0x26E07A3D)),
                        onClick = onReminder,
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.5.dp, WbBrandOrange),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = WbBrandOrange,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = stringResource(R.string.parent_reminder_cta),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WbBrandOrange,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.parent_footer_warm_quote),
                fontSize = 14.sp,
                color = WbTextMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(top = 12.dp),
            ) {
                AssetPhoto(
                    assetPath = WbAssetPhotos.PARENT_HOME_WATERMARK,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.1f },
                    contentScale = ContentScale.Fit,
                    placeholderColor = Color.Transparent,
                )
            }
        }
    }
}

private fun parentGreeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 0..10 -> "早上好"
        in 11..16 -> "下午好"
        else -> "晚上好"
    }
}
