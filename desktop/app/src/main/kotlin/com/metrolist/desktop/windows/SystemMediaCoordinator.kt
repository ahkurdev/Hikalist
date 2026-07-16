package com.metrolist.desktop.windows

import com.metrolist.desktop.player.DesktopMusicPlayer
import com.metrolist.desktop.player.RepeatMode

data class SystemMediaSnapshot(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val artwork: String?,
    val playbackState: DesktopMusicPlayer.State,
    val positionSeconds: Double,
    val durationSeconds: Double,
    val shuffle: Boolean,
    val repeatMode: RepeatMode,
)

data class SystemMediaMetadata(
    val mediaId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val artwork: String?,
)

data class SystemMediaTimeline(val positionSeconds: Double, val durationSeconds: Double)

data class SystemMediaModes(val shuffle: Boolean, val repeatMode: RepeatMode)

data class SystemMediaControls(
    val playPause: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
    val seekTo: (Double) -> Unit = {},
)

interface SystemMediaEventListener {
    fun onPlayPause()
    fun onNext()
    fun onPrevious()
    fun onSeek(positionSeconds: Double)
}

interface SystemMediaBackend : AutoCloseable {
    fun start(listener: SystemMediaEventListener)
    fun updateMetadata(metadata: SystemMediaMetadata)
    fun updatePlaybackState(state: DesktopMusicPlayer.State)
    fun updateTimeline(timeline: SystemMediaTimeline)
    fun updateModes(modes: SystemMediaModes) = Unit
}

class SystemMediaCoordinator(
    private val backend: SystemMediaBackend,
    private val controls: SystemMediaControls = SystemMediaControls(),
) : AutoCloseable {
    private var lastSnapshot: SystemMediaSnapshot? = null

    fun start() {
        backend.start(object : SystemMediaEventListener {
            override fun onPlayPause() = controls.playPause()
            override fun onNext() = controls.next()
            override fun onPrevious() = controls.previous()
            override fun onSeek(positionSeconds: Double) = controls.seekTo(positionSeconds)
        })
    }

    fun update(snapshot: SystemMediaSnapshot) {
        val previous = lastSnapshot
        if (previous?.mediaId != snapshot.mediaId || previous.title != snapshot.title ||
            previous.artist != snapshot.artist || previous.album != snapshot.album || previous.artwork != snapshot.artwork
        ) {
            backend.updateMetadata(
                SystemMediaMetadata(snapshot.mediaId, snapshot.title, snapshot.artist, snapshot.album, snapshot.artwork),
            )
        }
        if (previous?.playbackState != snapshot.playbackState) {
            backend.updatePlaybackState(snapshot.playbackState)
        }
        backend.updateTimeline(SystemMediaTimeline(snapshot.positionSeconds, snapshot.durationSeconds))
        if (previous?.shuffle != snapshot.shuffle || previous.repeatMode != snapshot.repeatMode) {
            backend.updateModes(SystemMediaModes(snapshot.shuffle, snapshot.repeatMode))
        }
        lastSnapshot = snapshot
    }

    override fun close() = backend.close()
}
