package com.nuvio.app.features.downloads

import java.io.File
import java.net.URI

internal actual class DownloadSubtitleStorage actual constructor(localVideoUri: String) {
    private val directory = File(
        runCatching { File(URI(localVideoUri)).path }.getOrElse { localVideoUri.removePrefix("file:") } + ".subtitles",
    )

    actual fun read(fileName: String): String? = path(fileName).takeIf { it.isFile }?.readText()

    actual fun write(fileName: String, text: String) {
        directory.mkdirs()
        path(fileName).writeText(text)
    }

    actual fun localFileUri(fileName: String): String? =
        path(fileName).takeIf { it.isFile }?.toURI()?.toString()

    actual fun remove() {
        directory.deleteRecursively()
    }

    private fun path(fileName: String): File {
        require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName && fileName != "." && fileName != "..")
        return File(directory, fileName)
    }
}
