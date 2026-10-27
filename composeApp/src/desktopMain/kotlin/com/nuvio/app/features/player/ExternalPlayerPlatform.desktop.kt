package com.nuvio.app.features.player

import java.io.File

// Desktop: hand the stream to a player installed on the PC (mpv, VLC or MPC-HC).
private data class DesktopExternalPlayer(
    val id: String,
    val name: String,
    val candidates: List<String>,
    val command: (exe: String, request: ExternalPlayerPlaybackRequest) -> List<String>,
)

private fun programFiles(vararg relative: String): List<String> {
    val roots = listOfNotNull(System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)"), System.getenv("LOCALAPPDATA"))
    return roots.flatMap { root -> relative.map { File(root, it).path } }
}

private val desktopExternalPlayers = listOf(
    DesktopExternalPlayer(
        id = "mpv",
        name = "mpv",
        candidates = programFiles("mpv\\mpv.exe", "Programs\\mpv\\mpv.exe") + listOf("/usr/bin/mpv", "/opt/homebrew/bin/mpv", "/usr/local/bin/mpv"),
        command = { exe, request ->
            buildList {
                add(exe)
                add("--force-media-title=${request.buildPlayerTitle(includeEpisodeTitle = true)}")
                if (request.resumePositionMs > 0) add("--start=${request.resumePositionMs / 1000}")
                if (request.sourceHeaders.isNotEmpty()) {
                    add("--http-header-fields=" + request.sourceHeaders.entries.joinToString(",") { "${it.key}: ${it.value}" })
                }
                request.subtitles.orEmpty().forEach { add("--sub-file=${it.url}") }
                add(request.sourceUrl)
            }
        },
    ),
    DesktopExternalPlayer(
        id = "vlc",
        name = "VLC",
        candidates = programFiles("VideoLAN\\VLC\\vlc.exe") + listOf("/usr/bin/vlc", "/Applications/VLC.app/Contents/MacOS/VLC"),
        command = { exe, request ->
            buildList {
                add(exe)
                add("--meta-title=${request.buildPlayerTitle(includeEpisodeTitle = true)}")
                if (request.resumePositionMs > 0) add("--start-time=${request.resumePositionMs / 1000}")
                request.sourceHeaders.entries.firstOrNull { it.key.equals("User-Agent", true) }?.let { add("--http-user-agent=${it.value}") }
                request.sourceHeaders.entries.firstOrNull { it.key.equals("Referer", true) }?.let { add("--http-referrer=${it.value}") }
                request.subtitles?.firstOrNull()?.let { add("--input-slave=${it.url}") }
                add(request.sourceUrl)
                add("--play-and-exit")
            }
        },
    ),
    DesktopExternalPlayer(
        id = "mpc-hc",
        name = "MPC-HC",
        candidates = programFiles("MPC-HC\\mpc-hc64.exe", "MPC-HC\\mpc-hc.exe", "K-Lite Codec Pack\\MPC-HC64\\mpc-hc64.exe"),
        command = { exe, request ->
            buildList {
                add(exe)
                add(request.sourceUrl)
                if (request.resumePositionMs > 0) { add("/startpos"); add("${request.resumePositionMs}") }
                add("/close")
            }
        },
    ),
)

private fun DesktopExternalPlayer.executable(): String? = candidates.firstOrNull { File(it).isFile }

internal actual object ExternalPlayerPlatform {
    actual fun defaultPlayerId(): String? = desktopExternalPlayers.firstOrNull { it.executable() != null }?.id

    actual fun availablePlayers(): List<ExternalPlayerApp> =
        desktopExternalPlayers.filter { it.executable() != null }.map { ExternalPlayerApp(it.id, it.name) }

    actual fun open(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerOpenResult = when (val intent = buildIntent(request, playerId)) {
        is ExternalPlayerIntentResult.Success -> runCatching {
            @Suppress("UNCHECKED_CAST")
            ProcessBuilder(intent.intent as List<String>).inheritIO().start()
            ExternalPlayerOpenResult.Opened
        }.getOrDefault(ExternalPlayerOpenResult.Failed)
        ExternalPlayerIntentResult.NotConfigured -> ExternalPlayerOpenResult.NoPlayerAvailable
        ExternalPlayerIntentResult.Failed -> ExternalPlayerOpenResult.Failed
    }

    actual fun buildIntent(
        request: ExternalPlayerPlaybackRequest,
        playerId: String?,
    ): ExternalPlayerIntentResult {
        val player = desktopExternalPlayers.firstOrNull { it.id == playerId && it.executable() != null }
            ?: desktopExternalPlayers.firstOrNull { it.executable() != null }
            ?: return ExternalPlayerIntentResult.NotConfigured
        val exe = player.executable() ?: return ExternalPlayerIntentResult.NotConfigured
        return ExternalPlayerIntentResult.Success(player.command(exe, request))
    }
}
