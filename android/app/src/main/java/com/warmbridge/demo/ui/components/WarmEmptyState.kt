package com.warmbridge.demo.ui.components



import androidx.compose.foundation.Image

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Spacer

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.height

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.size

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.graphics.painter.Painter

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.text.style.TextAlign

import androidx.compose.ui.unit.dp

import com.warmbridge.demo.ui.theme.WbDimens

import com.warmbridge.demo.ui.theme.WbTextSecondary



@Composable

fun WarmEmptyState(

    title: String,

    message: String,

    modifier: Modifier = Modifier,

    illustration: Painter? = null,

    assetPath: String? = null,

    assetPaths: List<String>? = null,

    actionLabel: String? = null,

    onAction: () -> Unit = {},

) {

    Column(

        modifier = modifier

            .fillMaxWidth()

            .padding(WbDimens.screenPadding),

        horizontalAlignment = Alignment.CenterHorizontally,

        verticalArrangement = Arrangement.Center,

    ) {

        when {

            illustration != null -> {

                Image(

                    painter = illustration,

                    contentDescription = null,

                    modifier = Modifier.size(120.dp),

                )

            }

            !assetPaths.isNullOrEmpty() -> {

                AssetPhotoFirstAvailable(

                    assetPaths = assetPaths,

                    modifier = Modifier.size(120.dp),

                    contentScale = ContentScale.Fit,

                    showPlaceholder = false,

                )

            }

            !assetPath.isNullOrBlank() -> {

                AssetPhoto(

                    assetPath = assetPath,

                    modifier = Modifier.size(120.dp),

                    contentScale = ContentScale.Fit,

                    showPlaceholder = false,

                )

            }

        }

        Text(

            text = title,

            style = MaterialTheme.typography.headlineSmall,

            textAlign = TextAlign.Center,

        )

        Spacer(Modifier.height(8.dp))

        Text(

            text = message,

            style = MaterialTheme.typography.bodyMedium,

            color = WbTextSecondary,

            textAlign = TextAlign.Center,

        )

        if (!actionLabel.isNullOrBlank()) {

            Spacer(Modifier.height(WbDimens.sectionGap))

            WarmPrimaryButton(

                onClick = onAction,

                modifier = Modifier.fillMaxWidth(),

            ) {

                Text(actionLabel)

            }

        }

    }

}

