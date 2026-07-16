package com.metrolist.desktop.sync

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncStoreTest {
    @Test
    fun `playlist upsert is ordered before songs created in the same second`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-order").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile) { 100L }
        store.enqueue(operation("a-song", SyncEntityType.SONG, "playlist-1", "song-1"))
        store.enqueue(operation("z-playlist", SyncEntityType.PLAYLIST, "playlist-1", "playlist-1"))

        assertEquals(listOf(SyncEntityType.PLAYLIST, SyncEntityType.SONG), store.pending().map { it.entityType })
    }

    @Test
    fun `playlist tombstone removes stale song operations for that playlist`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-delete-playlist").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile) { 100L }
        store.enqueue(operation("song-operation", SyncEntityType.SONG, "playlist-1", "song-1"))
        store.enqueue(
            operation("playlist-delete", SyncEntityType.PLAYLIST, "playlist-1", "playlist-1").copy(
                action = SyncAction.DELETE,
            ),
        )

        val pending = store.pending()
        assertEquals(1, pending.size)
        assertEquals("playlist-delete", pending.single().id)
    }

    @Test
    fun `string sync state survives store recreation`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-state").resolve("metadata.db").toFile()
        PlaylistSyncStore(databaseFile).saveState("bootstrap", "complete")

        assertEquals("complete", PlaylistSyncStore(databaseFile).loadState("bootstrap"))
    }

    @Test
    fun `outbox retries idempotently and removes acknowledged operations`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-outbox").resolve("metadata.db").toFile()
        var now = 100L
        val store = PlaylistSyncStore(databaseFile) { now }
        val operation = SyncOperation(
            id = "operation-1",
            entityType = SyncEntityType.PLAYLIST,
            action = SyncAction.UPSERT,
            playlistId = "playlist-1",
            entityId = "playlist-1",
            payload = "{\"name\":\"Morning\"}",
        )

        store.enqueue(operation)
        store.enqueue(operation)
        assertEquals(listOf(operation), store.pending())

        store.markFailed(operation.id, "offline")
        assertTrue(store.pending().isEmpty())

        now = 102L
        val retry = store.pending().single()
        assertEquals(1, retry.attemptCount)
        assertEquals("offline", retry.lastError)

        store.markSucceeded(operation.id)
        assertTrue(store.pending().isEmpty())
    }

    @Test
    fun `sync cursors persist independently for each remote table`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-cursors").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile)

        store.saveCursor(SyncCursor.PLAYLISTS, 21L)
        store.saveCursor(SyncCursor.SONGS, 34L)

        assertEquals(21L, store.loadCursor(SyncCursor.PLAYLISTS))
        assertEquals(34L, store.loadCursor(SyncCursor.SONGS))
        assertEquals(0L, store.loadCursor(SyncCursor.MEMBERS))
    }

    @Test
    fun `newer operation replaces stale pending work for the same entity`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-coalesce").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile) { 100L }
        val first = SyncOperation(
            id = "operation-first",
            entityType = SyncEntityType.PLAYLIST,
            action = SyncAction.UPSERT,
            playlistId = "playlist-1",
            entityId = "playlist-1",
            payload = "{\"name\":\"Old\"}",
        )
        val latest = first.copy(id = "operation-latest", payload = "{\"name\":\"Latest\"}")

        store.enqueue(first)
        store.enqueue(latest)

        assertEquals(listOf(latest), store.pending())
    }

    @Test
    fun `switching accounts resets remote cursors so older cloud data is not skipped`() {
        val databaseFile = Files.createTempDirectory("hikalist-sync-account").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile)
        store.prepareForUser("user-one")
        store.saveCursor(SyncCursor.PLAYLISTS, 99L)
        store.saveCursor(SyncCursor.MEMBERS, 88L)
        store.saveCursor(SyncCursor.SONGS, 77L)

        store.prepareForUser("user-two")

        assertEquals(SyncCursors(), store.loadCursors())
        assertEquals("user-two", store.loadState("active_sync_user"))
    }

    private fun operation(
        id: String,
        entityType: SyncEntityType,
        playlistId: String,
        entityId: String,
    ) = SyncOperation(
        id = id,
        entityType = entityType,
        action = SyncAction.UPSERT,
        playlistId = playlistId,
        entityId = entityId,
        payload = "{}",
    )
}
