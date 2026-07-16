package com.metrolist.desktop.offline

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineMediaStoreTest {
    @Test
    fun `committed m4a and metadata survive a new store instance`() {
        val root = Files.createTempDirectory("hikalist-offline-store").toFile()
        val store = OfflineMediaStore(root)
        val temporaryAudio = store.temporaryAudioFile("song/id")
        temporaryAudio.parentFile.mkdirs()
        temporaryAudio.writeBytes(byteArrayOf(1, 2, 3, 4))

        store.commit(
            metadata = OfflineSongMetadata(
                videoId = "song/id",
                title = "Offline song",
                artist = "Offline artist",
                album = "Offline album",
                thumbnailUrl = "https://example.com/cover.jpg",
                durationSeconds = 245,
            ),
            temporaryAudio = temporaryAudio,
            lyricsAvailable = true,
        )

        val restored = OfflineMediaStore(root).find("song/id")
        requireNotNull(restored)
        assertTrue(restored.audioFile.isFile)
        assertEquals("m4a", restored.audioFile.extension)
        assertEquals(4L, restored.audioFile.length())
        assertEquals("Offline song", restored.metadata.title)
        assertEquals("Offline artist", restored.metadata.artist)
        assertTrue(restored.lyricsAvailable)
    }

    @Test
    fun `missing audio is not reported as downloaded`() {
        val root = Files.createTempDirectory("hikalist-offline-missing").toFile()
        val store = OfflineMediaStore(root)
        val temporaryAudio = store.temporaryAudioFile("song-1")
        temporaryAudio.parentFile.mkdirs()
        temporaryAudio.writeText("audio")
        store.commit(
            OfflineSongMetadata("song-1", "Title", "Artist", null, null, 180),
            temporaryAudio,
            lyricsAvailable = false,
        )

        requireNotNull(store.find("song-1")).audioFile.delete()

        assertNull(store.find("song-1"))
        assertFalse(store.isDownloaded("song-1"))
    }

    @Test
    fun `remove deletes the whole offline song directory`() {
        val root = Files.createTempDirectory("hikalist-offline-remove").toFile()
        val store = OfflineMediaStore(root)
        val temporaryAudio = store.temporaryAudioFile("song-1")
        temporaryAudio.parentFile.mkdirs()
        temporaryAudio.writeText("audio")
        store.commit(
            OfflineSongMetadata("song-1", "Title", "Artist", null, null, 180),
            temporaryAudio,
            lyricsAvailable = false,
        )

        store.remove("song-1")

        assertFalse(store.songDirectory("song-1").exists())
        assertNull(store.find("song-1"))
    }
}
