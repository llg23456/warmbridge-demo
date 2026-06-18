package com.warmbridge.demo.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.warmbridge.demo.R

enum class WarmTopBarNavigation { Back, Close }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmTopAppBar(
    title: String,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier,
    navigation: WarmTopBarNavigation = WarmTopBarNavigation.Back,
    navigationEnabled: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(onClick = onNavigate, enabled = navigationEnabled) {
                when (navigation) {
                    WarmTopBarNavigation.Back -> Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                    )
                    WarmTopBarNavigation.Close -> Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close),
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
