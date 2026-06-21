package com.warmbridge.demo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.components.AssetPhoto
import com.warmbridge.demo.ui.components.RoleSelectCard
import com.warmbridge.demo.ui.components.WbAssetPhotos
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbPageBg
import com.warmbridge.demo.ui.theme.WbTextPrimary
import com.warmbridge.demo.ui.theme.WbTextSecondary

private val RoleSelectLogoRadius = 20.dp
private val RoleSelectLogoSize = 72.dp
private val RoleSelectCardHeight = 104.dp

@Composable
fun RoleSelectScreen(
    onParent: () -> Unit,
    onChild: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WbPageBg),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AssetPhoto(
                assetPath = WbAssetPhotos.ROLE_SELECT_HERO,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                placeholderColor = WbPageBg,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color(0x00000000),
                                0.72f to Color(0x00000000),
                                0.92f to Color(0xCCF7F5F2),
                                1f to WbPageBg,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = WbDimens.screenPadding)
                    .padding(top = 120.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(RoleSelectLogoRadius),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(RoleSelectLogoSize),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(RoleSelectLogoRadius))
                                .graphicsLayer {
                                    scaleX = 1.45f
                                    scaleY = 1.45f
                                },
                            contentScale = ContentScale.Crop,
                        )
                    }
                    Column(
                        modifier = Modifier.padding(start = 10.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.role_select_app_name),
                            style = MaterialTheme.typography.displaySmall,
                            color = WbTextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start,
                            fontSize = 35.sp,
                        )
                        Text(
                            text = stringResource(R.string.role_select_app_name_en),
                            style = MaterialTheme.typography.displaySmall,
                            color = WbBrandOrange,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp),
                            textAlign = TextAlign.Start,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.role_select_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WbTextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = WbDimens.screenPadding)
                .padding(bottom = 32.dp),
        ) {
            RoleSelectCard(
                avatarAssetPath = WbAssetPhotos.ROLE_SELECT_AVATAR_PARENT,
                title = stringResource(R.string.role_select_parent),
                subtitle = stringResource(R.string.role_select_parent_subtitle),
                onClick = onParent,
                modifier = Modifier
                    .fillMaxWidth()
                    
                    .height(RoleSelectCardHeight),
                contentDescription = stringResource(
                    R.string.role_select_parent,
                ) + "，" + stringResource(R.string.role_select_parent_subtitle),
            )
            Spacer(Modifier.height(12.dp))
            RoleSelectCard(
                avatarAssetPath = WbAssetPhotos.ROLE_SELECT_AVATAR_CHILD,
                title = stringResource(R.string.role_select_child),
                subtitle = stringResource(R.string.role_select_child_subtitle),
                onClick = onChild,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RoleSelectCardHeight),
                contentDescription = stringResource(
                    R.string.role_select_child,
                ) + "，" + stringResource(R.string.role_select_child_subtitle),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = WbTextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.role_select_switch_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = WbTextSecondary,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}
