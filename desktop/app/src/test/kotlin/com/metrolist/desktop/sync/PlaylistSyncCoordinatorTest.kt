package com.metrolist.desktop.sync

import java.nio.file.Files
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistSyncCoordinatorTest {
    @Test
    fun `start syncs immediately and enqueue triggers another sync without waiting for poll`() = runBlocking {
        val databaseFile = Files.createTempDirectory("hikalist-sync-coordinator").resolve("metadata.db").toFile()
        val store = PlaylistSyncStore(databaseFile)
        val remote = CountingRemote()
        val coordinator = PlaylistSyncCoordinator(
            store = store,
            remote = remote,
            applyRemote = {},
            pollInterval = 1.hours,
        )
        store.enqueue(operation("one"))

        coordinator.start()
        waitUntil { remote.pushed.size == 1 && coordinator.status.value.state == SyncConnectionState.SYNCED }

        coordinator.enqueue(operation("two"))
        waitUntil {
            remote.pushed.size == 2 && coordinator.status.value.pendingOperations == 0
        }

        assertEquals(0, coordinator.status.value.pendingOperations)
        assertTrue(remote.pullCount >= 2)
        coordinator.close()
    }

    private suspend fun waitUntil(predicate: () -> Boolean) = withTimeout(3_000L) {
        while (!predicate()) delay(10L)
    }

    private fun operation(id: String) = SyncOperation(
        id = id,
        entityType = SyncEntityType.PLAYLIST,
        action = SyncAction.UPSERT,
        playlistId = "playlist-$id",
        entityId = "playlist-$id",
        payload = "{}",
    )

    private class CountingRemote : PlaylistSyncRemote {
        val pushed = mutableListOf<SyncOperation>()
        var pullCount = 0

        override suspend fun ensureAuthenticated() = SyncIdentity("user", "You")

        override suspend fun push(operation: SyncOperation, identity: SyncIdentity) {
            pushed += operation
        }

        override suspend fun pull(cursors: SyncCursors): RemoteSyncBatch {
            pullCount += 1
            return RemoteSyncBatch()
        }
    }
}
