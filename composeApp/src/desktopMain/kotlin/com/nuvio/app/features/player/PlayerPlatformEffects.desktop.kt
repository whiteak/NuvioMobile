package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
actual fun LockPlayerToLandscape() = Unit

@Composable
actual fun FullscreenPlayerDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = content,
    )
}

@Composable
actual fun HidePlayerSystemBars() = Unit

@Composable
actual fun EnterImmersivePlayerMode(keepScreenAwake: Boolean) = Unit

@Composable
actual fun ManagePlayerPictureInPicture(
    isPlaying: Boolean,
    videoSize: IntSize,
) = Unit

@Composable
actual fun rememberIsInPictureInPicture(): Boolean = false

// Brightness and volume swipes are a touch-screen thing; the desktop uses keys and the mouse wheel.
@Composable
internal actual fun rememberPlatformPlayerGestureController(): PlayerGestureController? = null
