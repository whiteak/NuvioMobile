package com.nuvio.app.features.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nuvio.app.desktop.DesktopVlc
import com.nuvio.app.desktop.NSUserDefaults
import java.io.File
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import uk.co.caprica.vlcj.media.MediaSlaveType
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.base.TrackDescription
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat

// Built-in desktop player: libVLC decodes (including embedded and added subtitles, which it
// draws into the picture) and hands each frame to Compose, so the app's own controls sit on top.
@Composable
actual fun PlatformPlayerSurface(
    sourceUrl: String,
    sourceAudioUrl: String?,
    sourceHeaders: Map<String, String>,
    sourceResponseHeaders: Map<String, String>,
    externalSubtitles: List<com.nuvio.app.features.streams.StreamSubtitle>,
    streamType: String?,
    useYoutubeChunkedPlayback: Boolean,
    modifier: Modifier,
    playWhenReady: Boolean,
    initialPositionMs: Long?,
    initialPositionRequestKey: String?,
    resizeMode: PlayerResizeMode,
    playbackEngine: AndroidPlaybackEngine?,
    useNativeController: Boolean,
    onInitialPositionHandled: (key: String, handled: Boolean) -> Unit,
    onControllerReady: (PlayerEngineController) -> Unit,
    onSnapshot: (PlayerPlaybackSnapshot) -> Unit,
    onError: (String?) -> Unit,
) {
    val factory = DesktopVlc.factory
    if (factory == null) {
        LaunchedEffect(sourceUrl) {
            onSnapshot(PlayerPlaybackSnapshot(isLoading = false))
            onError(DESKTOP_PLAYER_MISSING_MESSAGE)
        }
        Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(DESKTOP_PLAYER_MISSING_MESSAGE, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
        }
        return
    }

    val currentOnSnapshot by rememberUpdatedState(onSnapshot)
    val currentOnError by rememberUpdatedState(onError)
    val frames = remember { DesktopFrameSink() }
    val player = remember {
        factory.mediaPlayers().newEmbeddedMediaPlayer().also { mediaPlayer ->
            mediaPlayer.videoSurface().set(factory.videoSurfaces().newVideoSurface(frames, frames, true))
        }
    }
    val controller = remember(player) { DesktopVlcController(player) }

    DisposableEffect(player) {
        player.events().addMediaPlayerEventListener(controller.events)
        onDispose {
            player.events().removeMediaPlayerEventListener(controller.events)
            // Stopping libVLC can block for a moment, so it happens off the UI thread.
            Thread({
                runCatching { player.controls().stop() }
                runCatching { player.release() }
            }, "vlc-release").start()
        }
    }

    LaunchedEffect(sourceUrl, sourceAudioUrl) {
        val startKey = initialPositionRequestKey
        val startMs = initialPositionMs?.takeIf { it > 0 }
        controller.open(
            url = sourceUrl,
            audioUrl = sourceAudioUrl,
            headers = sanitizePlaybackHeaders(sourceHeaders),
            startMs = startMs,
            externalSubtitles = externalSubtitles.map { it.url },
            playWhenReady = playWhenReady,
        )
        onControllerReady(controller)
        if (startKey != null) onInitialPositionHandled(startKey, startMs != null)
    }

    // Sound sync is remembered across videos: Bluetooth lag belongs to the headphones, not the film.
    LaunchedEffect(player) {
        val defaults = NSUserDefaults.standardUserDefaults
        PlayerAudioSync.enable(defaults.integerForKey(AUDIO_DELAY_KEY).toInt()) { defaults.setInteger(it, forKey = AUDIO_DELAY_KEY) }
        PlayerAudioSync.delayMs.collect { controller.setAudioDelayMs(it) }
    }

    LaunchedEffect(playWhenReady) {
        if (playWhenReady) controller.play() else controller.pause()
    }

    // Playback state for the app's controls, a few times a second.
    LaunchedEffect(player) {
        var reportedError = false
        while (true) {
            if (controller.failed.get() && !reportedError) {
                reportedError = true
                currentOnError(DESKTOP_PLAYER_ERROR_MESSAGE)
            }
            currentOnSnapshot(controller.snapshot())
            delay(250)
        }
    }

    var frame by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(frames) {
        while (true) {
            withFrameNanos { }
            frames.takeFrame()?.let { frame = it }
        }
    }

    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        frame?.let { image ->
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = when (resizeMode) {
                    PlayerResizeMode.Fit -> ContentScale.Fit
                    PlayerResizeMode.Fill -> ContentScale.FillBounds
                    PlayerResizeMode.Zoom -> ContentScale.Crop
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private const val DESKTOP_PLAYER_MISSING_MESSAGE =
    "The built-in player couldn't start because VLC's playback library is missing. Reinstall the app, or choose VLC under Settings › Playback › External player."

private const val AUDIO_DELAY_KEY = "player.audioDelayMs"

private const val DESKTOP_PLAYER_ERROR_MESSAGE = "This video couldn't be played."

// Largest frame size handed to Compose; bigger videos are scaled down by libVLC to keep copying cheap.
private const val MAX_FRAME_WIDTH = 1920
private const val MAX_FRAME_HEIGHT = 1080

/** Receives frames on libVLC's thread and passes the newest one to the UI thread. */
private class DesktopFrameSink : BufferFormatCallback, RenderCallback {
    private val lock = Any()
    private var width = 0
    private var height = 0
    private var pixels = ByteArray(0)
    private var dirty = false
    private var bitmap: Bitmap? = null

    override fun getBufferFormat(sourceWidth: Int, sourceHeight: Int): BufferFormat {
        val scale = minOf(1.0, MAX_FRAME_WIDTH.toDouble() / sourceWidth, MAX_FRAME_HEIGHT.toDouble() / sourceHeight)
        val w = max(2, (sourceWidth * scale).roundToInt())
        val h = max(2, (sourceHeight * scale).roundToInt())
        synchronized(lock) {
            width = w
            height = h
            pixels = ByteArray(w * h * 4)
            dirty = false
        }
        return RV32BufferFormat(w, h)
    }

    override fun allocatedBuffers(buffers: Array<out ByteBuffer>) = Unit

    override fun display(mediaPlayer: MediaPlayer, nativeBuffers: Array<out ByteBuffer>, bufferFormat: BufferFormat) {
        val buffer = nativeBuffers.firstOrNull() ?: return
        synchronized(lock) {
            if (buffer.remaining() < pixels.size) return
            buffer.get(pixels, 0, pixels.size)
            buffer.rewind()
            dirty = true
        }
    }

    /** Called on the UI thread; returns a new image when a frame arrived since the last call. */
    fun takeFrame(): ImageBitmap? = synchronized(lock) {
        if (!dirty || width == 0) return null
        dirty = false
        // libVLC's RV32 frames are laid out B, G, R, A.
        val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
        val target = bitmap ?: Bitmap().also { bitmap = it }
        target.installPixels(info, pixels, width * 4)
        target.asComposeImageBitmap()
    }
}

private class DesktopVlcController(private val player: EmbeddedMediaPlayer) : PlayerEngineController {
    val failed = AtomicBoolean(false)
    private val ended = AtomicBoolean(false)
    @Volatile private var buffering = true
    @Volatile private var lastUrl: String? = null
    @Volatile private var audioDelayUs = 0L
    @Volatile private var subtitleDelayUs = 0L
    private var lastOptions: Array<String> = emptyArray()

    val events = object : MediaPlayerEventAdapter() {
        override fun opening(mediaPlayer: MediaPlayer) {
            buffering = true
        }

        override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
            buffering = newCache < 100f
        }

        override fun playing(mediaPlayer: MediaPlayer) {
            buffering = false
            ended.set(false)
            // libVLC forgets delays when a new video starts; put them back (off its event thread).
            mediaPlayer.submit {
                mediaPlayer.audio().setDelay(audioDelayUs)
                mediaPlayer.subpictures().setDelay(subtitleDelayUs)
            }
        }

        override fun finished(mediaPlayer: MediaPlayer) {
            ended.set(true)
        }

        override fun error(mediaPlayer: MediaPlayer) {
            failed.set(true)
        }
    }

    fun open(
        url: String,
        audioUrl: String?,
        headers: Map<String, String>,
        startMs: Long?,
        externalSubtitles: List<String>,
        playWhenReady: Boolean,
    ) {
        failed.set(false)
        ended.set(false)
        buffering = true
        val options = buildList {
            headers.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }
                ?.let { add(":http-user-agent=${it.value}") }
            headers.entries.firstOrNull { it.key.equals("Referer", ignoreCase = true) }
                ?.let { add(":http-referrer=${it.value}") }
            audioUrl?.takeIf { it.isNotBlank() }?.let { add(":input-slave=$it") }
            startMs?.let { add(":start-time=${it / 1000.0}") }
        }.toTypedArray()
        lastUrl = url
        lastOptions = options
        val mrl = mediaLocation(url)
        if (playWhenReady) player.media().play(mrl, *options) else player.media().startPaused(mrl, *options)
        externalSubtitles.filter { it.isNotBlank() }.forEach {
            player.media().addSlave(MediaSlaveType.SUBTITLE, mediaLocation(it), false)
        }
    }

    fun snapshot(): PlayerPlaybackSnapshot {
        val status = player.status()
        val state = status.state()
        val dimension = runCatching { player.video().videoDimension() }.getOrNull()
        val length = status.length().coerceAtLeast(0L)
        val time = status.time().coerceAtLeast(0L)
        val isEnded = ended.get() || state == State.ENDED
        return PlayerPlaybackSnapshot(
            isLoading = !isEnded && !failed.get() && (state == State.OPENING || (buffering && state != State.PAUSED)),
            isPlaying = status.isPlaying,
            isEnded = isEnded,
            durationMs = length,
            positionMs = if (isEnded && length > 0) length else time,
            bufferedPositionMs = time,
            playbackSpeed = status.rate().takeIf { it > 0f } ?: 1f,
            videoWidth = dimension?.width ?: 0,
            videoHeight = dimension?.height ?: 0,
        )
    }

    override fun play() {
        if (ended.get() || player.status().state() == State.ENDED) {
            ended.set(false)
            player.controls().setTime(0)
        }
        player.controls().play()
    }

    override fun pause() = player.controls().setPause(true)

    override fun seekTo(positionMs: Long) {
        ended.set(false)
        player.controls().setTime(positionMs.coerceAtLeast(0L))
    }

    override fun seekBy(offsetMs: Long) = seekTo(player.status().time() + offsetMs)

    override fun retry() {
        val url = lastUrl ?: return
        val resumeAt = player.status().time()
        failed.set(false)
        ended.set(false)
        buffering = true
        val options = lastOptions.filterNot { it.startsWith(":start-time=") } +
            listOfNotNull(resumeAt.takeIf { it > 0 }?.let { ":start-time=${it / 1000.0}" })
        player.media().play(mediaLocation(url), *options.toTypedArray())
    }

    override fun setPlaybackSpeed(speed: Float) {
        player.controls().setRate(speed)
    }

    override fun setMuted(muted: Boolean) = player.audio().setMute(muted)

    override fun getAudioTracks(): List<AudioTrack> {
        val selected = player.audio().track()
        return player.audio().trackDescriptions().realTracks().mapIndexed { index, track ->
            AudioTrack(
                index = index,
                id = track.id().toString(),
                label = track.description() ?: "Track ${index + 1}",
                language = languageOf(track.description()),
                isSelected = track.id() == selected,
            )
        }
    }

    override fun getSubtitleTracks(): List<SubtitleTrack> {
        val selected = player.subpictures().track()
        return player.subpictures().trackDescriptions().realTracks().mapIndexed { index, track ->
            val label = track.description() ?: "Subtitle ${index + 1}"
            SubtitleTrack(
                index = index,
                id = track.id().toString(),
                label = label,
                language = languageOf(label),
                isSelected = track.id() == selected,
                isForced = label.contains("forced", ignoreCase = true),
            )
        }
    }

    override fun applyAudioLanguagePreferences(languages: List<String>) {
        val tracks = player.audio().trackDescriptions().realTracks()
        val match = languages.asSequence()
            .mapNotNull { language -> tracks.firstOrNull { matchesLanguage(it.description(), language) } }
            .firstOrNull() ?: return
        if (player.audio().track() != match.id()) player.audio().setTrack(match.id())
    }

    override fun selectAudioTrack(index: Int) {
        player.audio().trackDescriptions().realTracks().getOrNull(index)?.let { player.audio().setTrack(it.id()) }
    }

    override fun selectSubtitleTrack(index: Int) {
        if (index < 0) {
            player.subpictures().setTrack(-1)
            return
        }
        player.subpictures().trackDescriptions().realTracks().getOrNull(index)
            ?.let { player.subpictures().setTrack(it.id()) }
    }

    override fun setSubtitleUri(url: String) {
        player.media().addSlave(MediaSlaveType.SUBTITLE, mediaLocation(url), true)
    }

    override fun clearExternalSubtitle() {
        player.subpictures().setTrack(-1)
    }

    override fun clearExternalSubtitleAndSelect(trackIndex: Int) = selectSubtitleTrack(trackIndex)

    override fun applySubtitlePreferences(
        preferredLanguage: String,
        secondaryPreferredLanguage: String?,
        useForcedSubtitles: Boolean,
        autoSelectionApplied: Boolean,
        hasActiveSubtitle: Boolean,
        useCustomSubtitles: Boolean,
    ) {
        if (autoSelectionApplied || hasActiveSubtitle || useCustomSubtitles) return
        val tracks = player.subpictures().trackDescriptions().realTracks()
        val match = listOfNotNull(preferredLanguage, secondaryPreferredLanguage)
            .filter { it.isNotBlank() && !it.equals("none", ignoreCase = true) }
            .asSequence()
            .mapNotNull { language ->
                tracks.filter { matchesLanguage(it.description(), language) }
                    .sortedBy { it.description().orEmpty().contains("forced", ignoreCase = true) != useForcedSubtitles }
                    .firstOrNull()
            }
            .firstOrNull() ?: return
        player.subpictures().setTrack(match.id())
    }

    fun setAudioDelayMs(delayMs: Int) {
        // libVLC counts in microseconds; positive plays the sound later.
        audioDelayUs = delayMs * 1000L
        player.audio().setDelay(audioDelayUs)
    }

    override fun setSubtitleDelayMs(delayMs: Int) {
        subtitleDelayUs = delayMs * 1000L
        player.subpictures().setDelay(subtitleDelayUs)
    }
}

private fun List<TrackDescription>.realTracks(): List<TrackDescription> = filter { it.id() >= 0 }

/** libVLC wants file paths as file: URIs; web addresses pass through. */
private fun mediaLocation(url: String): String {
    if (url.contains("://")) return url
    val file = File(url)
    return if (file.isAbsolute) file.toURI().toString() else url
}

/** libVLC labels tracks like "Track 1 - [English]"; pull out the bracketed language. */
private fun languageOf(description: String?): String? =
    description?.let { Regex("\\[([^\\]]+)]").find(it)?.groupValues?.get(1) }

private fun matchesLanguage(description: String?, language: String): Boolean {
    val text = description?.lowercase() ?: return false
    val code = language.trim().lowercase()
    if (code.isEmpty()) return false
    val name = Locale.forLanguageTag(code).getDisplayLanguage(Locale.ENGLISH).lowercase()
    return (name.isNotEmpty() && name != code && text.contains(name)) || Regex("\\b${Regex.escape(code)}\\b").containsMatchIn(text)
}
