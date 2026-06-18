package com.warmbridge.demo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.warmbridge.demo.R
import com.warmbridge.demo.ui.theme.WarmBridgeTheme
import com.warmbridge.demo.ui.theme.WbBrandOrange
import com.warmbridge.demo.ui.theme.WbDivider
import com.warmbridge.demo.ui.theme.WbTextMuted

private val TitleActionFontSize = 18.sp
private  val TitleActionFontSizeB=13.sp
private val TitleActionIconSize = 18.dp

sealed interface WarmHomeGroupCardTitleAction {
    data class SeeMore(
        val label: String,
        val onClick: () -> Unit,
    ) : WarmHomeGroupCardTitleAction
}

@Composable
fun WarmHomeGroupCard(
    title: String,
    modifier: Modifier = Modifier,
    titleAction: WarmHomeGroupCardTitleAction? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    fontSize = TitleActionFontSize,
                    fontWeight = FontWeight.Bold,
                    color = WbBrandOrange,
                    modifier = Modifier.weight(1f),
                )
                when (val action = titleAction) {
                    is WarmHomeGroupCardTitleAction.SeeMore -> {
                        Row(
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = action.onClick,
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = action.label,
                                fontSize = TitleActionFontSizeB,
                                color = WbTextMuted,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = stringResource(R.string.cd_forward),
                                tint = WbTextMuted,
                                modifier = Modifier.size(TitleActionIconSize),
                            )
                        }
                    }
                    null -> Unit
                }
            }
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                color = WbDivider,
                thickness = 0.5.dp,
            )
            content()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WarmHomeGroupCardPreview() {
    WarmBridgeTheme {
        WarmHomeGroupCard(
            title = "精选内容",
            titleAction = WarmHomeGroupCardTitleAction.SeeMore("查看更多", {}),
            modifier = Modifier.padding(16.dp),
        ) {
            Text("卡片内容", modifier = Modifier.padding(16.dp))
        }
    }
}
