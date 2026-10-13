package com.nuvio.app.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.edition_home_banner
import org.jetbrains.compose.resources.painterResource

/** Ethereal: whether the banner runs edge to edge under the status bar (Requiem's lace banner sits below it). */
const val EDITION_BANNER_FULL_BLEED = true

/** Ethereal: the edition's artwork banner at the top of Home (each edition ships its own image). */
@Composable
fun EditionHomeBanner(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(bottom = if (EDITION_BANNER_FULL_BLEED) 0.dp else 8.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.edition_home_banner),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
            contentScale = ContentScale.FillWidth,
        )
    }
}
