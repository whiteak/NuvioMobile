package com.nuvio.app.core.auth

internal actual fun currentDeviceClientMetadata(): DeviceClientMetadata {
    val deviceName = (System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME") ?: runCatching {
        java.net.InetAddress.getLocalHost().hostName
    }.getOrNull()).orEmpty().trim().ifBlank { "Desktop" }
    return DeviceClientMetadata(
        deviceName = deviceName,
        platform = "${System.getProperty("os.name")} ${System.getProperty("os.version")}".trim(),
    )
}
