package com.metrolist.desktop.windows

import com.metrolist.desktop.player.DesktopMusicPlayer
import org.junit.Assert.assertEquals
import org.junit.Test

class SystemMediaCoordinatorTest {
    @Test
    fun `metadata state and timeline changes are forwarded`() {
        val backend = RecordingSystemMediaBackend()
        val coordinator = SystemMediaCoordinator(backend)
        val snapshot = SystemMediaSnapshot(
            mediaId = "video-id",
            title = "Ada",
            artist = "Lyodra, Afgan",
            album = "Ada",
            artwork = "https://example.com/cover.jpg",
            playbackState = DesktopMusicPlayer.State.PLAYING,
            positionSeconds = 25.0,
            durationSeconds = 211.0,
            shuffle = false,
            repeatMode = com.metrolist.desktop.player.RepeatMode.ALL,
        )

        coordinator.update(snapshot)
        coordinator.update(snapshot.copy(positionSeconds = 26.0))
        coordinator.update(snapshot.copy(playbackState = DesktopMusicPlayer.State.PAUSED, positionSeconds = 26.0))

        assertEquals(1, backend.metadata.size)
        assertEquals(2, backend.playbackStates.size)
        assertEquals(3, backend.timelines.size)
    }

    @Test
    fun `system buttons route to player actions`() {
        val actions = mutableListOf<String>()
        val backend = RecordingSystemMediaBackend()
        SystemMediaCoordinator(
            backend = backend,
            controls = SystemMediaControls(
                playPause = { actions += "playPause" },
                next = { actions += "next" },
                previous = { actions += "previous" },
                seekTo = { actions += "seek:$it" },
            ),
        ).start()

        backend.listener?.onPlayPause()
        backend.listener?.onNext()
        backend.listener?.onPrevious()
        backend.listener?.onSeek(42.5)

        assertEquals(listOf("playPause", "next", "previous", "seek:42.5"), actions)
    }
}

private class RecordingSystemMediaBackend : SystemMediaBackend {
    val metadata = mutableListOf<SystemMediaMetadata>()
    val playbackStates = mutableListOf<DesktopMusicPlayer.State>()
    val timelines = mutableListOf<SystemMediaTimeline>()
    var listener: SystemMediaEventListener? = null

    override fun start(listener: SystemMediaEventListener) {
        this.listener = listener
    }

    override fun updateMetadata(metadata: SystemMediaMetadata) {
        this.metadata += metadata
    }

    override fun updatePlaybackState(state: DesktopMusicPlayer.State) {
        playbackStates += state
    }

    override fun updateTimeline(timeline: SystemMediaTimeline) {
        timelines += timeline
    }

    override fun close() = Unit
}
