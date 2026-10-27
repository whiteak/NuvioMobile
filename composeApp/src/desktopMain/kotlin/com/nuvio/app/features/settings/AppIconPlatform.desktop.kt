package com.nuvio.app.features.settings

// Windows apps can't swap their own icon at run time.
internal actual object AppIconPlatform {
    actual val requiresCloseConfirmation: Boolean = false

    actual fun currentIconName(): String? = null

    actual suspend fun activateIcon(name: String?): Boolean = false
}
