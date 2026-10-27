package com.nuvio.app.features.watchprogress

import com.nuvio.app.desktop.NSUserDefaults

actual object ContinueWatchingEnrichmentStorage {
    actual fun loadPayload(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun savePayload(key: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = key)
    }

    actual fun removePayload(key: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }
}
