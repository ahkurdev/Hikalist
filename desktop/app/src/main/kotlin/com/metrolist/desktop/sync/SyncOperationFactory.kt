package com.metrolist.desktop.sync

import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.db.repository.SongRow
import com.metrolist.desktop.playlist.PlaylistSongMetadata
import com.metrolist.desktop.playlist.PlaylistSongMetadataStore
import java.util.UUID

class SyncOperationFactory(
    private val idProvider: () -> String = { UUID.randomUUID().toString() },
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
) {
    fun likedPlaylistUpsert(): SyncOperation = SyncOperation(
        id = idProvider(),
        entityType = SyncEntityType.PLAYLIST,
        action = SyncAction.UPSERT,
        playlistId = LikedSongsSync.LOCAL_PLAYLIST_ID,
        entityId = LikedSongsSync.LOCAL_PLAYLIST_ID,
        payload = SyncPayloadCodec.encode(
            PlaylistSyncPayload(
                name = "Liked Songs",
                description = "Every song you love, kept in one place.",
                coverUrl = null,
                deletedAtEpochSeconds = null,
            ),
        ),
    )

    fun playlistUpsert(playlist: PlaylistRow, description: String): SyncOperation = SyncOperation(
        id = idProvider(),
        entityType = SyncEntityType.PLAYLIST,
        action = SyncAction.UPSERT,
        playlistId = playlist.id,
        entityId = playlist.id,
        payload = SyncPayloadCodec.encode(
            PlaylistSyncPayload(
                name = playlist.name,
                description = description,
                coverUrl = playlist.thumbnailUrl,
                deletedAtEpochSeconds = null,
            ),
        ),
    )

    fun playlistDelete(playlistId: String): SyncOperation = tombstone(
        entityType = SyncEntityType.PLAYLIST,
        playlistId = playlistId,
        entityId = playlistId,
    )

    fun songUpsert(
        playlistId: String,
        song: SongRow,
        position: Int,
        metadata: PlaylistSongMetadata?,
    ): SyncOperation = SyncOperation(
        id = idProvider(),
        entityType = SyncEntityType.SONG,
        action = SyncAction.UPSERT,
        playlistId = playlistId,
        entityId = song.id,
        payload = SyncPayloadCodec.encode(
            SongSyncPayload(
                title = song.title,
                artist = song.artists.orEmpty().joinToString(", ").ifBlank { "Unknown artist" },
                album = song.albumName ?: song.album,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration.coerceAtLeast(0),
                position = position.coerceAtLeast(0),
                addedByName = metadata?.addedBy ?: PlaylistSongMetadataStore.LOCAL_CONTRIBUTOR,
                addedAtEpochSeconds = metadata?.addedAtEpochSeconds ?: nowEpochSeconds(),
                deletedAtEpochSeconds = null,
            ),
        ),
    )

    fun songDelete(playlistId: String, songId: String): SyncOperation = tombstone(
        entityType = SyncEntityType.SONG,
        playlistId = playlistId,
        entityId = songId,
    )

    private fun tombstone(
        entityType: SyncEntityType,
        playlistId: String,
        entityId: String,
    ) = SyncOperation(
        id = idProvider(),
        entityType = entityType,
        action = SyncAction.DELETE,
        playlistId = playlistId,
        entityId = entityId,
        payload = "{}",
    )
}
