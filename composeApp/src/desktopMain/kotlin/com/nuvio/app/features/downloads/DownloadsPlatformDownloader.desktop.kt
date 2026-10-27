package com.nuvio.app.features.downloads

import com.nuvio.app.desktop.DesktopPaths
import java.awt.Desktop
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlin.concurrent.thread

// Downloads go to Videos\<app> (or ~/Videos/<app>) and resume from a .part file.
internal actual object DownloadsPlatformDownloader {
    private val directory: File by lazy {
        val home = System.getProperty("user.home")
        File(File(home, "Videos"), DesktopPaths.appName).apply { mkdirs() }
    }

    actual fun start(
        request: DownloadPlatformRequest,
        onProgress: (downloadedBytes: Long, totalBytes: Long?) -> Unit,
        onSuccess: (localFileUri: String, totalBytes: Long?) -> Unit,
        onFailure: (message: String) -> Unit,
        onPaused: () -> Unit,
    ): DownloadsTaskHandle {
        val cancelled = java.util.concurrent.atomic.AtomicBoolean(false)
        val target = File(directory, request.destinationFileName)
        val part = File(directory, request.destinationFileName + ".part")
        thread(isDaemon = true, name = "download-${request.destinationFileName}") {
            try {
                var existing = if (part.isFile) part.length() else 0L
                val connection = (URL(request.sourceUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    request.sourceHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                    if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
                }
                val code = connection.responseCode
                if (code !in 200..299) {
                    onFailure("HTTP $code")
                    return@thread
                }
                if (code == 200) existing = 0L
                val length = connection.getHeaderFieldLong("Content-Length", -1L)
                val total = if (length >= 0) existing + length else null
                RandomAccessFile(part, "rw").use { out ->
                    out.setLength(existing)
                    out.seek(existing)
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(256 * 1024)
                        var done = existing
                        var lastReport = 0L
                        while (true) {
                            if (cancelled.get()) {
                                onPaused()
                                return@thread
                            }
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            done += read
                            val now = System.currentTimeMillis()
                            if (now - lastReport > 500) {
                                lastReport = now
                                onProgress(done, total)
                            }
                        }
                    }
                }
                target.delete()
                if (!part.renameTo(target)) error("Couldn't finish the file")
                onSuccess(target.toURI().toString(), target.length())
            } catch (e: Exception) {
                if (cancelled.get()) onPaused() else onFailure(e.message ?: "Download failed")
            }
        }
        return object : DownloadsTaskHandle {
            override fun cancel() {
                cancelled.set(true)
            }
        }
    }

    actual fun restoreItem(item: DownloadItem): DownloadItem = item

    actual fun removeFile(localFileUri: String?): Boolean =
        localFileUri?.let { fileOf(it) }?.let { !it.exists() || it.delete() } ?: false

    actual fun removePartialFile(destinationFileName: String): Boolean =
        File(directory, "$destinationFileName.part").let { !it.exists() || it.delete() }

    actual fun resolveLocalFileUri(localFileUri: String?, destinationFileName: String): String? {
        localFileUri?.let { fileOf(it) }?.takeIf { it.isFile }?.let { return it.toURI().toString() }
        return File(directory, destinationFileName).takeIf { it.isFile }?.toURI()?.toString()
    }

    actual fun openDownloadsDirectory(): Boolean = runCatching {
        Desktop.getDesktop().open(directory)
        true
    }.getOrDefault(false)

    private fun fileOf(uri: String): File? = runCatching { File(URI(uri)) }.getOrNull()
}
