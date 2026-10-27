package com.nuvio.app.features.mdblist

import com.nuvio.app.desktop.DesktopSecrets

internal actual object PlatformMdbListAuthPersistence : MdbListAuthPersistence {
    private const val PREFIX = "mdblist.profile."

    actual override fun read(profileId: Int): String? = DesktopSecrets.read("$PREFIX$profileId")

    actual override fun write(profileId: Int, value: String?) = DesktopSecrets.write("$PREFIX$profileId", value)

    actual override fun clear() = DesktopSecrets.clear(PREFIX)
}
