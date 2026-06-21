package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbImageExplainBg
import com.warmbridge.demo.ui.theme.WbTextPrimary

private val HeroHeight = 176.dp
private val HeadlineWidthFraction = 0.62f
private val HeroBottomCornerRadius = 20.dp

@Composable
fun VideoQuickHeroSection(
    modifier: Modifier = Modifier,
) {
    val heroShape = RoundedCornerShape(bottomStart = HeroBottomCornerRadius, bottomEnd = HeroBottomCornerRadius)
    val headline = stringResource(R.string.video_quick_headline)
    val highlight = stringResource(R.string.video_quick_headline_highlight)
    val headlineText = buildAnnotatedString {
        val highlightStart = headline.indexOf(highlight)
        if (highlightStart >= 0) {
            append(headline.substring(0, highlightStart))
            withStyle(SpanStyle(color = WbBrandOrange, fontWeight = FontWeight.SemiBold)) {
                append(highlight)
            }
            append(headline.substring(highlightStart + highlight.length))
        } else {
            append(headline)
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .clip(heroShape),

    ) {
        AssetPhoto(
            assetPath = WbAssetPhotos.VIDEO_QUICK_HERO,
            modifier = Modifier.matchParentSize()
                ,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.CenterEnd,
            placeholderColor = WbImageExplainBg,
        )
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(HeadlineWidthFraction)
                .padding(start = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = headlineText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = MaterialTheme.typography.titleLarge.fontSize * 1.35f,
                ),
                color = WbTextPrimary,
                fontSize =15.sp
            )
        }
    }
}
