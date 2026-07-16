package com.metrolist.desktop.windows

import com.metrolist.desktop.player.DesktopMusicPlayer
import com.metrolist.desktop.player.RepeatMode
import io.github.selemba1000.JMTC
import io.github.selemba1000.JMTCCallbacks
import io.github.selemba1000.JMTCEnabledButtons
import io.github.selemba1000.JMTCMediaType
import io.github.selemba1000.JMTCMusicProperties
import io.github.selemba1000.JMTCParameters
import io.github.selemba1000.JMTCPlayingState
import io.github.selemba1000.JMTCSettings
import io.github.selemba1000.JMTCTimelineProperties
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.security.MessageDigest
import org.slf4j.LoggerFactory

class JmtcSystemMediaBackend private constructor(
    private val session: JMTC,
    private val artworkCache: ArtworkFileCache,
) : SystemMediaBackend {
    private val logger = LoggerFactory.getLogger(JmtcSystemMediaBackend::class.java)

    override fun start(listener: SystemMediaEventListener) {
        val callbacks = JMTCCallbacks().apply {
            onPlay = { listener.onPlayPause() }
            onPause = { listener.onPlayPause() }
            onNext = { listener.onNext() }
            onPrevious = { listener.onPrevious() }
            onSeek = { milliseconds -> listener.onSeek(milliseconds / 1_000.0) }
        }
        session.setCallbacks(callbacks)
        session.setEnabledButtons(JMTCEnabledButtons(true, true, false, true, true))
        session.setMediaType(JMTCMediaType.Music)
        session.setEnabled(true)
    }

    override fun updateMetadata(metadata: SystemMediaMetadata) {
        val artwork = artworkCache.resolve(metadata.artwork)
        session.setMediaProperties(
            JMTCMusicProperties(
                metadata.title,
                metadata.artist,
                metadata.album.orEmpty(),
                metadata.artist,
                emptyArray(),
                0,
                0,
                artwork,
            ),
        )
        session.updateDisplay()
    }

    override fun updatePlaybackState(state: DesktopMusicPlayer.State) {
        session.setPlayingState(
            when (state) {
                DesktopMusicPlayer.State.PLAYING -> JMTCPlayingState.PLAYING
                DesktopMusicPlayer.State.PAUSED -> JMTCPlayingState.PAUSED
                DesktopMusicPlayer.State.BUFFERING -> JMTCPlayingState.CHANGING
                DesktopMusicPlayer.State.IDLE,
                DesktopMusicPlayer.State.ERROR,
                -> JMTCPlayingState.STOPPED
            },
        )
    }

    override fun updateTimeline(timeline: SystemMediaTimeline) {
        val durationMs = (timeline.durationSeconds.coerceAtLeast(0.0) * 1_000.0).toLong()
        val positionMs = (timeline.positionSeconds.coerceIn(0.0, timeline.durationSeconds.coerceAtLeast(0.0)) * 1_000.0).toLong()
        session.setTimelineProperties(JMTCTimelineProperties(0L, durationMs, 0L, durationMs))
        session.setPosition(positionMs)
    }

    override fun updateModes(modes: SystemMediaModes) {
        val loopStatus = when (modes.repeatMode) {
            RepeatMode.OFF -> JMTCParameters.LoopStatus.None
            RepeatMode.ALL -> JMTCParameters.LoopStatus.Playlist
            RepeatMode.ONE -> JMTCParameters.LoopStatus.Track
        }
        session.setParameters(JMTCParameters(loopStatus, 1.0, 1.0, modes.shuffle))
    }

    override fun close() {
        runCatching { session.setPlayingState(JMTCPlayingState.CLOSED) }
            .onFailure { logger.debug("Could not close Windows media state", it) }
        runCatching { session.resetDisplay() }
        runCatching { session.setEnabled(false) }
    }

    companion object {
        fun createOrNull(): JmtcSystemMediaBackend? {
            if (!System.getProperty("os.name").contains("win", ignoreCase = true)) return null
            return runCatching {
                JmtcSystemMediaBackend(
                    session = requireNotNull(JMTC.getInstance(JMTCSettings("Hikalist", "hikalist"))),
                    artworkCache = ArtworkFileCache(),
                )
            }.onFailure {
                LoggerFactory.getLogger(JmtcSystemMediaBackend::class.java)
                    .warn("Windows media controls are unavailable; playback will continue normally", it)
            }.getOrNull()
        }
    }
}

internal class ArtworkFileCache(
    private val directory: File = File(System.getProperty("java.io.tmpdir"), "hikalist-media-art"),
) {
    fun resolve(source: String?): File? {
        if (source.isNullOrBlank()) return null
        val local = File(source)
        if (local.isFile) return local
        if (!source.startsWith("http://") && !source.startsWith("https://")) return null
        return runCatching {
            directory.mkdirs()
            val name = MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray())
                .joinToString("") { "%02x".format(it) }
            val destination = File(directory, "$name.jpg")
            if (!destination.isFile || destination.length() == 0L) {
                val connection = URI(source).toURL().openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                    setRequestProperty("User-Agent", "Hikalist Desktop")
                }
                connection.getInputStream().use { input ->
                    Files.newOutputStream(destination.toPath()).use(input::copyTo)
                }
            }
            destination
        }.getOrNull()
    }
}
