package com.metrolist.desktop.storage

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageManagerTest {
    @Test
    fun `snapshot counts downloads and disposable cache separately`() {
        val root = Files.createTempDirectory("hikalist-storage").toFile()
        val downloads = root.resolve("offline").apply { mkdirs() }
        val lyrics = root.resolve("lyrics").apply { mkdirs() }
        val artwork = root.resolve("artwork").apply { mkdirs() }
        downloads.resolve("audio.m4a").writeBytes(ByteArray(7))
        lyrics.resolve("lyrics.lrc").writeBytes(ByteArray(3))
        artwork.resolve("cover.jpg").writeBytes(ByteArray(5))

        val snapshot = StorageManager(downloads, listOf(lyrics, artwork)).snapshot()

        assertEquals(7L, snapshot.downloadBytes)
        assertEquals(8L, snapshot.cacheBytes)
    }

    @Test
    fun `clear cache preserves downloads and files outside managed cache roots`() {
        val root = Files.createTempDirectory("hikalist-clear-cache").toFile()
        val downloads = root.resolve("offline").apply { mkdirs() }
        val lyrics = root.resolve("lyrics").apply { mkdirs() }
        val covers = root.resolve("covers").apply { mkdirs() }
        downloads.resolve("audio.m4a").writeText("audio")
        lyrics.resolve("lyrics.lrc").writeText("lyrics")
        covers.resolve("playlist.jpg").writeText("cover")
        val manager = StorageManager(downloads, listOf(lyrics))

        manager.clearCache()

        assertTrue(downloads.resolve("audio.m4a").isFile)
        assertTrue(covers.resolve("playlist.jpg").isFile)
        assertFalse(lyrics.resolve("lyrics.lrc").exists())
    }

    @Test
    fun `clear downloads preserves cache`() {
        val root = Files.createTempDirectory("hikalist-clear-downloads").toFile()
        val downloads = root.resolve("offline").apply { mkdirs() }
        val lyrics = root.resolve("lyrics").apply { mkdirs() }
        downloads.resolve("audio.m4a").writeText("audio")
        lyrics.resolve("lyrics.lrc").writeText("lyrics")
        val manager = StorageManager(downloads, listOf(lyrics))

        manager.clearDownloads()

        assertFalse(downloads.resolve("audio.m4a").exists())
        assertTrue(lyrics.resolve("lyrics.lrc").isFile)
        assertTrue(downloads.isDirectory)
    }
}
