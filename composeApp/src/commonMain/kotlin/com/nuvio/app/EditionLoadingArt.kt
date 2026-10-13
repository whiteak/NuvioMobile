package com.nuvio.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.edition_loading_art
import org.jetbrains.compose.resources.painterResource

/** Ethereal: full-screen edition artwork behind the startup loading screens (each edition ships its own image). */
@Composable
internal fun EditionLoadingArt(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(Res.drawable.edition_loading_art),
        contentDescription = null,
        modifier = modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}
