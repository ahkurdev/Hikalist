package com.metrolist.desktop.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LikedSongsSyncTest {
    @Test
    fun `liked playlist remote id is private to each account and maps back locally`() {
        val first = LikedSongsSync.remotePlaylistId("user-one")
        val second = LikedSongsSync.remotePlaylistId("user-two")

        assertTrue(first != second)
        assertEquals(LikedSongsSync.LOCAL_PLAYLIST_ID, LikedSongsSync.toLocalPlaylistId(first, "user-one"))
        assertEquals(second, LikedSongsSync.toLocalPlaylistId(second, "user-one"))
    }

    @Test
    fun `liked playlist operation has stable local identity`() {
        val operation = SyncOperationFactory(idProvider = { "operation" }).likedPlaylistUpsert()

        assertEquals(LikedSongsSync.LOCAL_PLAYLIST_ID, operation.playlistId)
        assertEquals("Liked Songs", SyncPayloadCodec.decodePlaylist(operation.payload).name)
    }
}
