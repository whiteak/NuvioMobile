package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlin.concurrent.thread

@Composable
actual fun rememberExternalPlayerLauncher(
    onResult: (ExternalPlaybackResult?) -> Unit,
): (ExternalPlayerIntentResult.Success) -> Boolean {
    val currentOnResult by rememberUpdatedState(onResult)
    return remember {
        { intentResult: ExternalPlayerIntentResult.Success ->
            @Suppress("UNCHECKED_CAST")
            val command = intentResult.intent as? List<String>
            if (command == null) {
                false
            } else {
                runCatching {
                    val process = ProcessBuilder(command).inheritIO().start()
                    // Desktop players don't report where you stopped, so just say playback ended.
                    thread(isDaemon = true, name = "external-player") {
                        process.waitFor()
                        currentOnResult(null)
                    }
                    true
                }.getOrDefault(false)
            }
        }
    }
}
