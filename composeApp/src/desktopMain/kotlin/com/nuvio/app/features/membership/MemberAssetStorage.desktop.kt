package com.nuvio.app.features.membership

import com.nuvio.app.desktop.DesktopPaths
import com.nuvio.app.desktop.NSUserDefaults
import java.io.File

internal actual object MemberAssetStorage {
    private const val accessPayloadKey = "member_access_payload"
    private const val backgroundCatalogPayloadKey = "member_background_catalog_payload"
    private val backgroundDirectory get() = File(DesktopPaths.cacheDir, "NuvioMemberBackgrounds")
    private val avatarDirectory get() = File(DesktopPaths.cacheDir, "NuvioMemberAvatars")

    actual fun loadAccessPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(accessPayloadKey)

    actual fun saveAccessPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = accessPayloadKey)
    }

    actual fun loadProfileBackgroundCatalogPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(backgroundCatalogPayloadKey)

    actual fun saveProfileBackgroundCatalogPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = backgroundCatalogPayloadKey)
    }

    actual fun loadProfileBackground(cacheKey: String): ByteArray? =
        File(backgroundDirectory, "${safeKey(cacheKey)}.png").takeIf { it.isFile }?.readBytes()

    actual fun saveProfileBackground(cacheKey: String, bytes: ByteArray) {
        write(File(backgroundDirectory, "${safeKey(cacheKey)}.png"), bytes)
    }

    actual fun loadProfileAvatar(cacheKey: String): String? =
        File(avatarDirectory, safeKey(cacheKey)).takeIf { it.isFile }?.toURI()?.toString()

    actual fun saveProfileAvatar(cacheKey: String, bytes: ByteArray): String? {
        val file = File(avatarDirectory, safeKey(cacheKey))
        return if (write(file, bytes)) file.toURI().toString() else null
    }

    actual fun clearAccess() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(accessPayloadKey)
    }

    private fun write(file: File, bytes: ByteArray): Boolean = runCatching {
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        true
    }.getOrDefault(false)

    private fun safeKey(value: String): String = value.map { character ->
        if (character.isLetterOrDigit() || character == '.' || character == '_' || character == '-') character else '_'
    }.joinToString("")
}
