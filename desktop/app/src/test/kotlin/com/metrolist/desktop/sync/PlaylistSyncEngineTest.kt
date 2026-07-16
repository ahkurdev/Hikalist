package com.metrolist.desktop.sync

import java.io.IOException
import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncEngineTest {
    @Test
    fun `successful sync pushes outbox then applies remote changes and advances cursors`() = runBlocking {
        val databaseFile = Files.createTempDirectory("hikalist-sync-engine").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile) { 500L }
        val operation = playlistOperation("operation-1")
        store.enqueue(operation)
        val batch = RemoteSyncBatch(
            playlists = listOf(remotePlaylist(changeSequence = 7L)),
            songs = listOf(remoteSong(changeSequence = 11L)),
            playlistCursor = 7L,
            songCursor = 11L,
        )
        val remote = FakeRemote(batch = batch)
        var applied: RemoteSyncBatch? = null
        val engine = PlaylistSyncEngine(store, remote) { applied = it }

        val result = engine.syncOnce()

        assertEquals(SyncRunResult.SYNCED, result)
        assertEquals(listOf(operation), remote.pushed)
        assertTrue(store.pending().isEmpty())
        assertEquals(batch, applied)
        assertEquals(7L, store.loadCursor(SyncCursor.PLAYLISTS))
        assertEquals(11L, store.loadCursor(SyncCursor.SONGS))
    }

    @Test
    fun `network failure keeps the operation queued for automatic retry`() = runBlocking {
        val databaseFile = Files.createTempDirectory("hikalist-sync-offline").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile) { 900L }
        store.enqueue(playlistOperation("operation-offline"))
        val remote = FakeRemote(pushFailure = IOException("network unavailable"))
        val engine = PlaylistSyncEngine(store, remote) {}

        val result = engine.syncOnce()

        assertEquals(SyncRunResult.OFFLINE, result)
        assertEquals(1, store.countPending())
    }

    @Test
    fun `missing account pauses sync without consuming queued operations`() = runBlocking {
        val databaseFile = Files.createTempDirectory("hikalist-sync-auth").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile)
        store.enqueue(playlistOperation("operation-auth"))
        val remote = FakeRemote(authenticationFailure = AuthRequiredException())
        val engine = PlaylistSyncEngine(store, remote) {}

        val result = engine.syncOnce()

        assertEquals(SyncRunResult.AUTH_REQUIRED, result)
        assertEquals(1, store.countPending())
    }

    private fun playlistOperation(id: String) = SyncOperation(
        id = id,
        entityType = SyncEntityType.PLAYLIST,
        action = SyncAction.UPSERT,
        playlistId = "playlist-1",
        entityId = "playlist-1",
        payload = "{\"name\":\"Morning\"}",
    )

    private fun remotePlaylist(changeSequence: Long) = RemotePlaylist(
        id = "playlist-1",
        name = "Morning",
        description = "",
        coverUrl = null,
        ownerId = "user-1",
        deletedAt = null,
        changeSequence = changeSequence,
    )

    private fun remoteSong(changeSequence: Long) = RemotePlaylistSong(
        playlistId = "playlist-1",
        songId = "song-1",
        title = "Song",
        artist = "Artist",
        album = null,
        thumbnailUrl = null,
        duration = 180,
        position = 0,
        addedBy = "user-1",
        addedByName = "You",
        addedAtEpochSeconds = 100L,
        deletedAt = null,
        changeSequence = changeSequence,
    )

    private class FakeRemote(
        private val batch: RemoteSyncBatch = RemoteSyncBatch(),
        private val pushFailure: Throwable? = null,
        private val authenticationFailure: Throwable? = null,
    ) : PlaylistSyncRemote {
        val pushed = mutableListOf<SyncOperation>()

        override suspend fun ensureAuthenticated(): SyncIdentity {
            authenticationFailure?.let { throw it }
            return SyncIdentity("user-1", "You")
        }

        override suspend fun push(operation: SyncOperation, identity: SyncIdentity) {
            pushFailure?.let { throw it }
            pushed += operation
        }

        override suspend fun pull(cursors: SyncCursors): RemoteSyncBatch = batch
    }
}
