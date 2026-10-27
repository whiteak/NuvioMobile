package com.nuvio.app.features.servers

import com.nuvio.app.desktop.DesktopSecrets

internal actual object ServerStorage {
    private const val PREFIX = "servers."

    actual fun read(key: String): String? = DesktopSecrets.read("$PREFIX$key")

    actual fun write(key: String, value: String?) = DesktopSecrets.write("$PREFIX$key", value)

    actual fun clear() = DesktopSecrets.clear(PREFIX)
}
