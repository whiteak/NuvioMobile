package com.nuvio.app.features.profiles

// Ethereal: avatars that ship with this edition (Requiem's apply.sh turns them off).
// The Nuvio server only knows its own avatar ids, so these are saved on the profile as an
// avatar URL, which syncs to the TV app and other devices like any custom avatar.
const val EDITION_AVATARS_ENABLED = true

private const val EditionAvatarBaseUrl =
    "https://raw.githubusercontent.com/whiteak/NuvioMobile/cmp-rewrite/editions/ethereal/avatars"

internal val EDITION_AVATARS: List<AvatarCatalogItem> = if (EDITION_AVATARS_ENABLED) {
    listOf(
        AvatarCatalogItem(
            id = "edition-mn",
            displayName = "MN",
            storagePath = "$EditionAvatarBaseUrl/mn.png",
            category = "edition",
            bgColor = "#FFFFFF",
        ),
    )
} else {
    emptyList()
}

internal fun editionAvatar(id: String?): AvatarCatalogItem? =
    id?.let { EDITION_AVATARS.firstOrNull { avatar -> avatar.id == it } }

internal fun editionAvatarForUrl(url: String?): AvatarCatalogItem? =
    url?.trim()?.let { EDITION_AVATARS.firstOrNull { avatar -> avatar.storagePath == it } }
