package com.nuvio.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import com.nuvio.app.core.ui.nuvio
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.edition_loading_art
import org.jetbrains.compose.resources.painterResource

/** Ethereal: how long the edition's opening picture stays up when the app starts. */
private const val EDITION_INTRO_MILLIS = 5_000L

private var editionIntroShown = false

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

/** Ethereal: holds the opening picture over the app for a few seconds on each cold start while it loads underneath. */
@Composable
internal fun EditionIntroOverlay() {
    var visible by remember { mutableStateOf(!editionIntroShown) }
    LaunchedEffect(Unit) {
        if (visible) {
            editionIntroShown = true
            delay(EDITION_INTRO_MILLIS)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(tween(450)),
    ) {
        AppLoadingContent(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.nuvio.colors.background)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
                },
        )
    }
}
