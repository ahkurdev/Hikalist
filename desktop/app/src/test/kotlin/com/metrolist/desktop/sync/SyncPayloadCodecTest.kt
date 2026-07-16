package com.metrolist.desktop.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPayloadCodecTest {
    @Test
    fun `playlist payload survives durable outbox serialization`() {
        val payload = PlaylistSyncPayload(
            name = "Morning",
            description = "Start the day",
            coverUrl = null,
            deletedAtEpochSeconds = null,
        )

        assertEquals(payload, SyncPayloadCodec.decodePlaylist(SyncPayloadCodec.encode(payload)))
    }

    @Test
    fun `song payload preserves attribution and ordering`() {
        val payload = SongSyncPayload(
            title = "Song",
            artist = "Artist",
            album = "Album",
            thumbnailUrl = "https://example.test/cover.jpg",
            duration = 180,
            position = 3,
            addedByName = "You",
            addedAtEpochSeconds = 123L,
            deletedAtEpochSeconds = null,
        )

        assertEquals(payload, SyncPayloadCodec.decodeSong(SyncPayloadCodec.encode(payload)))
    }
}
