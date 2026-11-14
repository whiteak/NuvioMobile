package com.nuvio.app.features.autosync

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.readAvailable
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

// The PC: the same Ktor requests as iOS (on OkHttp), and the JVM's charsets as on Android.

/** Container index range requests: short timeouts, like Android's dedicated client. */
private val containerIndexHttpClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        followRedirects = true
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 4_000
            requestTimeoutMillis = 5_000
        }
    }
}

/** Subtitle downloads. */
private val subtitleHttpClient: HttpClient by lazy {
    HttpClient(OkHttp) {
        followRedirects = true
        expectSuccess = false
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
            requestTimeoutMillis = 30_000
        }
    }
}

internal actual suspend fun <T> autoSyncHttpGet(
    url: String,
    headers: Map<String, String>,
    kind: AutoSyncHttpClientKind,
    timeoutMs: Long?,
    block: suspend (AutoSyncHttpResponse) -> T,
): T {
    val client = when (kind) {
        AutoSyncHttpClientKind.CONTAINER_INDEX -> containerIndexHttpClient
        AutoSyncHttpClientKind.SUBTITLE -> subtitleHttpClient
    }
    return client.prepareGet(url) {
        headers.forEach { (name, value) ->
            // Ktor refuses headers it manages itself (Content-Length, Transfer-Encoding, ...).
            if (!HttpHeaders.isUnsafe(name)) header(name, value)
        }
        if (timeoutMs != null) {
            timeout { requestTimeoutMillis = timeoutMs }
        }
    }.execute { response ->
        val channel = response.bodyAsChannel()
        block(
            AutoSyncHttpResponse(
                status = response.status.value,
                statusText = response.status.description,
                finalUrl = response.call.request.url.toString(),
                headers = response.headers.entries().associate { (name, values) ->
                    name.lowercase() to values.joinToString(",")
                },
                body = object : AutoSyncHttpBody {
                    override suspend fun read(buffer: ByteArray, offset: Int, length: Int): Int =
                        channel.readAvailable(buffer, offset, length)
                },
            ),
        )
    }
}

internal actual fun autoSyncIsCharsetSupported(charsetName: String): Boolean =
    try {
        Charset.isSupported(charsetName)
    } catch (_: Exception) {
        false
    }

internal actual fun autoSyncDecodeBytes(
    bytes: ByteArray,
    offset: Int,
    length: Int,
    charsetName: String,
    strict: Boolean,
): String? {
    val charset = try {
        Charset.forName(charsetName)
    } catch (_: Exception) {
        return null
    }
    if (!strict) return String(bytes, offset, length, charset)
    return try {
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes, offset, length))
            .toString()
    } catch (_: Exception) {
        null
    }
}
