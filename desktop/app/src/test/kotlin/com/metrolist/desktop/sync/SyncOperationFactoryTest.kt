package com.metrolist.desktop.sync

import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.db.repository.SongRow
import com.metrolist.desktop.playlist.PlaylistSongMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SyncOperationFactoryTest {
    private val factory = SyncOperationFactory(
        idProvider = { "operation-id" },
        nowEpochSeconds = { 900L },
    )

    @Test
    fun `playlist upsert includes editable cloud metadata`() {
        val operation = factory.playlistUpsert(
            playlist = PlaylistRow(id = "playlist-1", name = "Morning", thumbnailUrl = "cover.jpg"),
            description = "Easy listening",
        )

        assertEquals(SyncEntityType.PLAYLIST, operation.entityType)
        assertEquals(SyncAction.UPSERT, operation.action)
        assertEquals("playlist-1", operation.entityId)
        val payload = SyncPayloadCodec.decodePlaylist(operation.payload)
        assertEquals("Morning", payload.name)
        assertEquals("Easy listening", payload.description)
        assertEquals("cover.jpg", payload.coverUrl)
        assertNull(payload.deletedAtEpochSeconds)
    }

    @Test
    fun `song upsert preserves contributor and ordering metadata`() {
        val operation = factory.songUpsert(
            playlistId = "playlist-1",
            song = SongRow(
                id = "song-1",
                title = "Song",
                duration = 180,
                thumbnailUrl = "thumb.jpg",
                albumName = "Album",
                artists = listOf("Artist", "Guest"),
            ),
            position = 3,
            metadata = PlaylistSongMetadata("playlist-1", "song-1", "allan", 123L),
        )

        val payload = SyncPayloadCodec.decodeSong(operation.payload)
        assertEquals("Artist, Guest", payload.artist)
        assertEquals("allan", payload.addedByName)
        assertEquals(123L, payload.addedAtEpochSeconds)
        assertEquals(3, payload.position)
    }

    @Test
    fun `delete operations are tombstones without requiring old payload`() {
        val operation = factory.songDelete("playlist-1", "song-1")

        assertEquals(SyncAction.DELETE, operation.action)
        assertEquals("{}", operation.payload)
    }
}
