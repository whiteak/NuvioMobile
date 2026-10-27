package com.nuvio.app.core.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal actual object EpisodeReleaseDatePlatform {
    actual fun nowEpochMs(): Long = System.currentTimeMillis()

    actual fun localIsoDateAtEpochMs(epochMs: Long): String? =
        DateTimeFormatter.ISO_LOCAL_DATE.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))

    actual fun localDateTimeToEpochMs(normalizedIsoDateTime: String): Long? = runCatching {
        LocalDateTime.parse(normalizedIsoDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}
