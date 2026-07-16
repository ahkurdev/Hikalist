package com.metrolist.desktop.ui

import com.metrolist.desktop.offline.OfflineDownloadState
import com.metrolist.desktop.player.DesktopMusicPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadUiPolicyTest {
    @Test
    fun `queue item keeps playback metadata in offline request`() {
        val request = DesktopMusicPlayer.QueueItem(
            videoId = "song-1",
            playlistId = "playlist-1",
            title = "Title",
            artist = "Artist",
            album = "Album",
            thumbnailUrl = "https://example.com/cover.jpg",
            duration = 181,
        ).toOfflineDownloadRequest()

        assertEquals("song-1", request.metadata.videoId)
        assertEquals("playlist-1", request.playlistId)
        assertEquals("Title", request.metadata.title)
        assertEquals(181, request.metadata.durationSeconds)
    }

    @Test
    fun `playlist summary reports completed and active songs`() {
        val summary = playlistDownloadSummary(
            videoIds = listOf("one", "two", "three"),
            states = mapOf(
                "two" to OfflineDownloadState.Downloading(downloadedBytes = 50, totalBytes = 100),
            ),
            isDownloaded = { it == "one" },
        )

        assertEquals(1, summary.completed)
        assertEquals(3, summary.total)
        assertTrue(summary.active)
        assertFalse(summary.allDownloaded)
    }
}
