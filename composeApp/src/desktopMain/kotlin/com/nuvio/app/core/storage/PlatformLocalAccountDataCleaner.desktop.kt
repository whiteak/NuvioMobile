package com.nuvio.app.core.storage

import com.nuvio.app.desktop.DesktopPaths
import com.nuvio.app.desktop.NSUserDefaults
import java.io.File

// Signing out of the account wipes everything the desktop app saved for it.
internal actual object PlatformLocalAccountDataCleaner {
    actual fun wipe() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.dictionaryRepresentation().keys.filterIsInstance<String>().forEach(defaults::removeObjectForKey)
        File(DesktopPaths.dataDir, "nuvio_plugin_scrapers").deleteRecursively()
        File(DesktopPaths.dataDir, "NuvioMembership").deleteRecursively()
    }
}
