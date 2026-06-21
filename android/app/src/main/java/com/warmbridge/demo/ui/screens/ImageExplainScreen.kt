package com.warmbridge.demo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.warmbridge.demo.ui.theme.WbSurface
import com.warmbridge.demo.R
import com.warmbridge.demo.data.remote.NetworkModule
import com.warmbridge.demo.ui.components.ImageExplainHeroSection
import com.warmbridge.demo.ui.components.ImageExplainPickZone
import com.warmbridge.demo.ui.components.ImageExplainPrivacyBanner
import com.warmbridge.demo.ui.components.WarmLoadingContent
import com.warmbridge.demo.ui.components.WarmStatusBanner
import com.warmbridge.demo.ui.components.WarmStatusBannerType
import com.warmbridge.demo.ui.components.WarmToolScreenScaffold
import com.warmbridge.demo.ui.theme.WbImageExplainBg
import com.warmbridge.demo.util.buildImageUploadPart
import com.warmbridge.demo.util.copyUriToSessionCover
import com.warmbridge.demo.util.humanizeNetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ImageExplainScreen(
    onBack: () -> Unit,
    onDoneToDetail: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    val upload: (Uri) -> Unit = { uri ->
        scope.launch {
            loading = true
            err = null
            try {
                val part = withContext(Dispatchers.IO) {
                    buildImageUploadPart(context, uri)
                }
                val resp = NetworkModule.api.imageExplain(part)
                copyUriToSessionCover(context, resp.itemId, uri)
                onDoneToDetail(resp.itemId)
            } catch (e: Exception) {
                err = humanizeNetworkError(e) ?: context.getString(R.string.image_upload_fail)
            } finally {
                loading = false
            }
        }
    }

    val pick = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let(upload)
    }

    val launchPick = {
        err = null
        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    WarmToolScreenScaffold(
        title = stringResource(R.string.media_image_title),
        onNavigate = onBack,
        primaryLabel = stringResource(R.string.image_explain_primary),
        onPrimaryClick = launchPick,
        primaryEnabled = !loading,
        navigationEnabled = !loading,
        headerContent = {
            ImageExplainHeroSection()
        },
        primaryContent = {
            Icon(
                Icons.Filled.Image,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(stringResource(R.string.image_explain_primary), style = MaterialTheme.typography.labelLarge)
        },
        bottomBarPrefix = {
            ImageExplainPrivacyBanner()
        },
        primaryButtonBottomPadding = 28.dp,
        containerColor = WbImageExplainBg,
        topBarContainerColor = WbSurface,
        statusContent = {
            if (loading) {
                WarmLoadingContent(message = stringResource(R.string.image_uploading))
                Spacer(Modifier.height(12.dp))
            }
            err?.let { message ->
                WarmStatusBanner(message = message, type = WarmStatusBannerType.Error)
                Spacer(Modifier.height(12.dp))
            }
        },
    ) {
        ImageExplainPickZone(
            onClick = launchPick,
            enabled = !loading,
        )
    }
}
