package com.nuvio.app.features.downloads

// No lock-screen / live activity status on desktop.
internal actual object DownloadsLiveStatusPlatform {
    actual fun onItemsChanged(items: List<DownloadItem>) = Unit
}
