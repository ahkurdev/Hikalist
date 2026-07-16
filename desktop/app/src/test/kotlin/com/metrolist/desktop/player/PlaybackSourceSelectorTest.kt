package com.metrolist.desktop.player

import com.metrolist.desktop.offline.OfflineMediaStore
import com.metrolist.desktop.offline.OfflineSongMetadata
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSourceSelectorTest {
    @Test
    fun `downloaded song selects local m4a without resolving youtube`() = runBlocking {
        val store = OfflineMediaStore(Files.createTempDirectory("hikalist-player-offline").toFile())
        val temporaryAudio = store.temporaryAudioFile("song-1")
        temporaryAudio.parentFile.mkdirs()
        temporaryAudio.writeBytes(byteArrayOf(1, 2, 3))
        store.commit(
            OfflineSongMetadata("song-1", "Title", "Artist", null, null, 200),
            temporaryAudio,
            lyricsAvailable = true,
        )
        var networkCalled = false
        val selector = PlaybackSourceSelector(
            offlineMediaProvider = store::find,
            onlineResolver = {
                networkCalled = true
                error("Network must not be used")
            },
        )

        val source = selector.resolve(DesktopMusicPlayer.QueueItem(videoId = "song-1"))

        assertTrue(source is PlaybackSource.Local)
        assertEquals("m4a", (source as PlaybackSource.Local).audioFile.extension)
        assertEquals(200, source.durationSeconds)
        assertFalse(networkCalled)
    }

    @Test
    fun `song without download falls back to online resolver`() = runBlocking {
        val store = OfflineMediaStore(Files.createTempDirectory("hikalist-player-online").toFile())
        val selector = PlaybackSourceSelector(
            offlineMediaProvider = store::find,
            onlineResolver = {
                DesktopYTPlayerUtils.PlaybackData(
                    streamUrl = "https://example.com/audio",
                    mimeType = "audio/mp4; codecs=mp4a.40.2",
                    bitrate = 128_000,
                    itag = 140,
                    durationSeconds = 180,
                    title = "Title",
                    author = "Artist",
                    videoId = "song-2",
                    streamClient = "test",
                    userAgent = "Hikalist",
                )
            },
        )

        val source = selector.resolve(DesktopMusicPlayer.QueueItem(videoId = "song-2"))

        assertTrue(source is PlaybackSource.Online)
        assertEquals(180, source.durationSeconds)
    }
}
