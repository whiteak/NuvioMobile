package com.nuvio.app.features.settings

// Windows apps can't swap their own icon at run time.
internal actual object AppIconPlatform {
    actual val requiresCloseConfirmation: Boolean = false

    // Nothing to remember: the PC has only the standard icon.
    actual fun loadSelectedIconKey(): String? = null

    actual fun saveSelectedIconKey(key: String) {}

    actual fun currentIconName(): String? = null

    actual suspend fun activateIcon(name: String?): Boolean = false
}
