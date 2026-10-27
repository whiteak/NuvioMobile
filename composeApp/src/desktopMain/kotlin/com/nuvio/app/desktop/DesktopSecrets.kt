package com.nuvio.app.desktop

/**
 * Sign-in tokens and server keys. Kept with the other settings in the user's own app-data folder,
 * which only that Windows account can read.
 */
object DesktopSecrets {
    private const val ROOT = "secret."
    private val store get() = NSUserDefaults.standardUserDefaults

    fun read(key: String): String? = store.stringForKey(ROOT + key)

    fun write(key: String, value: String?) {
        if (value == null) store.removeObjectForKey(ROOT + key) else store.setObject(value, forKey = ROOT + key)
    }

    fun clear(prefix: String) {
        store.dictionaryRepresentation().keys
            .filterIsInstance<String>()
            .filter { it.startsWith(ROOT + prefix) }
            .forEach { store.removeObjectForKey(it) }
    }
}
