package com.metrolist.desktop.playlist

import java.nio.file.Files
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSongMetadataStoreTest {
    @Test
    fun `records the original contributor and date without overwriting them`() {
        val databaseFile = Files.createTempDirectory("hikalist-playlist-song-metadata").resolve("metadata.db").toFile()
        val store = PlaylistSongMetadataStore(databaseFile) { 1_752_537_600L }

        store.recordIfAbsent("playlist-1", "song-1", "You")
        store.recordIfAbsent("playlist-1", "song-1", "Another user")

        val metadata = store.getForPlaylist("playlist-1").getValue("song-1")
        assertEquals("You", metadata.addedBy)
        assertEquals(1_752_537_600L, metadata.addedAtEpochSeconds)
        assertEquals("Jul 15, 2025", PlaylistSongMetadataFormatter.date(metadata, ZoneOffset.UTC))
        assertEquals("You", PlaylistSongMetadataFormatter.addedBy(metadata))
    }

    @Test
    fun `removes song and playlist metadata independently`() {
        val databaseFile = Files.createTempDirectory("hikalist-playlist-song-metadata-cleanup").resolve("metadata.db").toFile()
        val store = PlaylistSongMetadataStore(databaseFile) { 1_000L }
        store.recordIfAbsent("playlist-1", "song-1", "You")
        store.recordIfAbsent("playlist-1", "song-2", "You")
        store.recordIfAbsent("playlist-2", "song-3", "You")

        store.remove("playlist-1", "song-1")
        assertFalse(store.getForPlaylist("playlist-1").containsKey("song-1"))
        assertTrue(store.getForPlaylist("playlist-1").containsKey("song-2"))

        store.deletePlaylist("playlist-1")
        assertTrue(store.getForPlaylist("playlist-1").isEmpty())
        assertTrue(store.getForPlaylist("playlist-2").containsKey("song-3"))
    }

    @Test
    fun `remote metadata can replace a stale local contributor`() {
        val databaseFile = Files.createTempDirectory("hikalist-playlist-song-metadata-remote").resolve("metadata.db").toFile()
        val store = PlaylistSongMetadataStore(databaseFile)
        store.recordIfAbsent("playlist-1", "song-1", "You", 100L)

        store.upsert("playlist-1", "song-1", "allan", 200L)

        val metadata = store.getForPlaylist("playlist-1").getValue("song-1")
        assertEquals("allan", metadata.addedBy)
        assertEquals(200L, metadata.addedAtEpochSeconds)
    }

    @Test
    fun `missing metadata uses a neutral fallback`() {
        assertEquals("—", PlaylistSongMetadataFormatter.addedBy(null))
        assertEquals("—", PlaylistSongMetadataFormatter.date(null, ZoneOffset.UTC))
    }
}
