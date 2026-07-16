package com.metrolist.desktop.sync

import java.io.IOException

class PlaylistSyncEngine(
    private val store: PlaylistSyncStore,
    private val remote: PlaylistSyncRemote,
    private val applyRemote: suspend (RemoteSyncBatch) -> Unit,
) {
    suspend fun syncOnce(): SyncRunResult {
        val identity = try {
            remote.ensureAuthenticated()
        } catch (error: Throwable) {
            return error.toRunResult()
        }
        store.prepareForUser(identity.userId)

        store.pending().forEach { operation ->
            try {
                remote.push(operation, identity)
                store.markSucceeded(operation.id)
            } catch (error: Throwable) {
                store.markFailed(operation.id, error.message ?: error::class.simpleName.orEmpty())
                return error.toRunResult()
            }
        }

        return try {
            val batch = remote.pull(store.loadCursors())
            applyRemote(batch)
            store.saveCursor(SyncCursor.PLAYLISTS, batch.playlistCursor)
            store.saveCursor(SyncCursor.MEMBERS, batch.memberCursor)
            store.saveCursor(SyncCursor.SONGS, batch.songCursor)
            SyncRunResult.SYNCED
        } catch (error: Throwable) {
            error.toRunResult()
        }
    }

    private fun Throwable.toRunResult(): SyncRunResult = when {
        this is AuthRequiredException -> SyncRunResult.AUTH_REQUIRED
        this is IOException -> SyncRunResult.OFFLINE
        this::class.qualifiedName.orEmpty().contains("Network", ignoreCase = true) -> SyncRunResult.OFFLINE
        this::class.qualifiedName.orEmpty().contains("Connect", ignoreCase = true) -> SyncRunResult.OFFLINE
        else -> SyncRunResult.ERROR
    }
}
