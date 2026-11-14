package com.nuvio.app.features.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import com.nuvio.app.desktop.DesktopPaths
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr

// The PC keeps imported subtitle fonts in the app's data folder, as iOS keeps them in Application Support.
internal actual object SubtitleFontStorage {
    actual fun defaultFamily(): FontFamily = FontFamily.SansSerif

    private val directory: File get() = File(DesktopPaths.dataDir, "subtitle-fonts")

    private fun file(name: String): File {
        require(name == "fonts.json" || name.matches(Regex("custom-[a-f0-9-]{36}")))
        return File(directory, name)
    }

    actual fun read(name: String): ByteArray? = readFontFile(file(name))

    actual fun write(name: String, bytes: ByteArray) {
        directory.mkdirs()
        val target = file(name)
        val temporary = File(directory, "${target.name}.tmp")
        temporary.writeBytes(bytes)
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    actual fun remove(name: String) {
        file(name).delete()
    }

    actual fun fontFamily(id: String): FontFamily? = read(id)?.let { bytes ->
        val data = Data.makeFromBytes(bytes)
        try {
            requireNotNull(FontMgr.default.makeFromData(data)).close()
        } finally {
            data.close()
        }
        FontFamily(Font(identity = id, data = bytes))
    }
}

private fun readFontFile(file: File): ByteArray? {
    if (!file.isFile) return null
    val size = file.length()
    if (size !in 1..MaxSubtitleFontBytes.toLong()) return null
    return file.readBytes()
}

@Composable
internal actual fun rememberSubtitleFontImporter(onResult: (Result<SubtitleFont>) -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val latestResult = rememberUpdatedState(onResult)
    return {
        scope.launch {
            val picked = withContext(Dispatchers.IO) {
                val dialog = FileDialog(null as Frame?, "Choose a font (.ttf or .otf)", FileDialog.LOAD).apply {
                    setFilenameFilter { _, name -> name.lowercase().let { it.endsWith(".ttf") || it.endsWith(".otf") } }
                    file = "*.ttf;*.otf"
                    isVisible = true
                }
                val name = dialog.file ?: return@withContext null
                File(dialog.directory, name)
            } ?: return@launch
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    SubtitleFonts.import(picked.name, requireNotNull(readFontFile(picked)))
                }
            }
            latestResult.value(result)
        }
    }
}
