package com.nuvio.app.desktop

import java.io.File
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.factory.discovery.strategy.NativeDiscoveryStrategy
import uk.co.caprica.vlcj.factory.discovery.strategy.WindowsNativeDiscoveryStrategy

/**
 * libVLC for the built-in player. The installer ships its own copy (app resources/vlc);
 * an installed 64-bit VLC is the fallback when running from Gradle.
 */
object DesktopVlc {
    val factory: MediaPlayerFactory? by lazy {
        runCatching {
            val discovery = NativeDiscovery(BundledVlcStrategy(bundledDir()), WindowsNativeDiscoveryStrategy())
            if (!discovery.discover()) return@runCatching null
            MediaPlayerFactory(discovery, "--no-video-title-show", "--quiet")
        }.onFailure { error ->
            System.err.println("libVLC could not be loaded: ${error.message}")
        }.getOrNull()
    }

    private fun bundledDir(): File? =
        System.getProperty("compose.application.resources.dir")?.let { File(it, "vlc") }
}

private class BundledVlcStrategy(private val dir: File?) : NativeDiscoveryStrategy {
    override fun supported(): Boolean = dir?.resolve("libvlc.dll")?.isFile == true

    override fun discover(): String? = dir?.takeIf { supported() }?.absolutePath

    override fun onFound(path: String): Boolean = true

    // libvlccore finds its plugins folder next to itself on Windows.
    override fun onSetPluginPath(path: String): Boolean = true
}
