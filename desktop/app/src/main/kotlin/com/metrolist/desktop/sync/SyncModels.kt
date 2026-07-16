package com.metrolist.desktop.sync

enum class SyncEntityType { PLAYLIST, SONG }

enum class SyncAction { UPSERT, DELETE }

enum class SyncCursor(val storageKey: String) {
    PLAYLISTS("cursor_playlists"),
    MEMBERS("cursor_members"),
    SONGS("cursor_songs"),
}

data class SyncOperation(
    val id: String,
    val entityType: SyncEntityType,
    val action: SyncAction,
    val playlistId: String,
    val entityId: String,
    val payload: String,
    val attemptCount: Int = 0,
    val lastError: String? = null,
)

data class SyncIdentity(
    val userId: String,
    val username: String,
)

data class SyncCursors(
    val playlists: Long = 0L,
    val members: Long = 0L,
    val songs: Long = 0L,
)

data class RemotePlaylist(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String?,
    val ownerId: String,
    val deletedAt: Long?,
    val changeSequence: Long,
)

data class RemotePlaylistSong(
    val playlistId: String,
    val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val duration: Int,
    val position: Int,
    val addedBy: String,
    val addedByName: String,
    val addedAtEpochSeconds: Long,
    val deletedAt: Long?,
    val changeSequence: Long,
)

data class RemoteSyncBatch(
    val playlists: List<RemotePlaylist> = emptyList(),
    val songs: List<RemotePlaylistSong> = emptyList(),
    val playlistCursor: Long = 0L,
    val memberCursor: Long = 0L,
    val songCursor: Long = 0L,
    val visiblePlaylistIds: Set<String>? = null,
)

enum class SyncRunResult { SYNCED, OFFLINE, AUTH_REQUIRED, ERROR }

enum class SyncConnectionState { STARTING, SYNCING, SYNCED, OFFLINE, AUTH_REQUIRED, ERROR }

class AuthRequiredException : IllegalStateException("Sign in to sync playlists")

data class PlaylistSyncStatus(
    val state: SyncConnectionState = SyncConnectionState.STARTING,
    val pendingOperations: Int = 0,
    val lastSyncedAtEpochSeconds: Long? = null,
    val message: String? = null,
)

interface PlaylistSyncRemote {
    suspend fun ensureAuthenticated(): SyncIdentity
    suspend fun push(operation: SyncOperation, identity: SyncIdentity)
    suspend fun pull(cursors: SyncCursors): RemoteSyncBatch
    suspend fun subscribe(onRemoteChange: suspend () -> Unit) = Unit
    suspend fun close() = Unit
}
