package com.warmbridge.demo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbDimens
import com.warmbridge.demo.ui.theme.WbMinePageBg
import com.warmbridge.demo.ui.theme.warmHomePageGradientBrush

/**
 * 双端首页共用壳：上半屏插画背景 + 白卡 overlap 内容区。
 */
@Composable
fun WarmHomePageShell(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerBackgroundHeight = screenHeight / 2

    Column(
        modifier
            .fillMaxSize()
            .background(WbMinePageBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(headerBackgroundHeight)
                    .background(warmHomePageGradientBrush()),
            ) {
                AssetPhoto(
                    assetPath = WbAssetPhotos.HOME_HEADER_BACKGROUND,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopCenter,
                    showPlaceholder = false,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    0.72f to Color.Transparent,
                                    1f to WbMinePageBg,
                                ),
                            ),
                        ),
                )
            }
            Column(Modifier.fillMaxWidth().statusBarsPadding()) {
                header()
                Column(
                    Modifier
                        .fillMaxWidth()
                        .offset(y = -WbDimens.pageContentOverlap)
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}
