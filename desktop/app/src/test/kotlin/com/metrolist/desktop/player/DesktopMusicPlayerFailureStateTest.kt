package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DesktopMusicPlayerFailureStateTest {
    @Test
    fun `missing playback engine publishes an actionable failure`() {
        val player = DesktopMusicPlayer(ffmpegPathProvider = { null })
        val item = DesktopMusicPlayer.QueueItem(
            videoId = "video-id",
            title = "Test Song",
            artist = "Test Artist",
            duration = 180,
        )

        try {
            player.play(item)

            assertEquals(DesktopMusicPlayer.State.ERROR, player.state.value)
            assertEquals(item.videoId, player.currentSong.value?.item?.videoId)
            val notice = player.playbackNotice.value
            assertNotNull(notice)
            assertTrue(notice is PlaybackNotice.Failed)
            assertEquals(PlaybackFailureKind.DECODER, (notice as PlaybackNotice.Failed).failure.kind)
        } finally {
            player.dispose()
        }
    }

    @Test
    fun `failure notice can be dismissed without clearing the queue`() {
        val player = DesktopMusicPlayer(ffmpegPathProvider = { null })
        val item = DesktopMusicPlayer.QueueItem(videoId = "video-id", title = "Test Song")

        try {
            player.play(item)
            player.dismissPlaybackNotice()

            assertNull(player.playbackNotice.value)
            assertEquals(item.videoId, player.queueItems.value.single().videoId)
        } finally {
            player.dispose()
        }
    }
}
