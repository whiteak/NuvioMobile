package com.nuvio.app.features.simkl

import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.desktop.DesktopSecrets
import com.nuvio.app.desktop.NSUserDefaults
import java.security.MessageDigest
import java.security.SecureRandom

internal actual object SimklPlatformClock {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()
}

internal actual object SimklPkceCrypto {
    private val random = SecureRandom()

    actual fun secureRandomBytes(size: Int): ByteArray {
        require(size > 0)
        return ByteArray(size).also(random::nextBytes)
    }

    actual fun sha256(value: ByteArray): ByteArray {
        require(value.isNotEmpty())
        return MessageDigest.getInstance("SHA-256").digest(value)
    }
}

internal actual object SimklAuthStorage {
    private const val METADATA_KEY = "simkl_auth_metadata"
    private const val ACCESS_TOKEN_KEY = "simkl_access_token"
    private const val CODE_VERIFIER_KEY = "simkl_code_verifier"

    actual fun loadMetadataPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(METADATA_KEY))

    actual fun saveMetadataPayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(METADATA_KEY))
    }

    actual fun loadAccessToken(): String? = DesktopSecrets.read(ProfileScopedKey.of(ACCESS_TOKEN_KEY))

    actual fun saveAccessToken(value: String?) =
        DesktopSecrets.write(ProfileScopedKey.of(ACCESS_TOKEN_KEY), value?.takeIf { it.isNotBlank() })

    actual fun loadCodeVerifier(): String? = DesktopSecrets.read(ProfileScopedKey.of(CODE_VERIFIER_KEY))

    actual fun saveCodeVerifier(value: String?) =
        DesktopSecrets.write(ProfileScopedKey.of(CODE_VERIFIER_KEY), value?.takeIf { it.isNotBlank() })

    actual fun removeProfile(profileId: Int) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(ProfileScopedKey.of(METADATA_KEY, profileId))
        DesktopSecrets.write(ProfileScopedKey.of(ACCESS_TOKEN_KEY, profileId), null)
        DesktopSecrets.write(ProfileScopedKey.of(CODE_VERIFIER_KEY, profileId), null)
    }
}
