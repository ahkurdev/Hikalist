package com.metrolist.desktop.sync

interface LocalPlaylistSyncTarget {
    fun upsertPlaylist(playlist: RemotePlaylist)
    fun deletePlaylist(playlistId: String)
    fun upsertSong(song: RemotePlaylistSong)
    fun deleteSong(playlistId: String, songId: String)
    fun reconcileVisiblePlaylists(visiblePlaylistIds: Set<String>)
    fun refresh()
}

class RemotePlaylistApplicator(
    private val target: LocalPlaylistSyncTarget,
) {
    fun apply(batch: RemoteSyncBatch) {
        val deletedPlaylistIds = batch.playlists
            .filter { it.deletedAt != null }
            .mapTo(mutableSetOf(), RemotePlaylist::id)
        batch.playlists.forEach { playlist ->
            if (playlist.deletedAt == null) target.upsertPlaylist(playlist)
            else target.deletePlaylist(playlist.id)
        }
        batch.songs.forEach { song ->
            if (song.deletedAt != null || song.playlistId in deletedPlaylistIds) {
                target.deleteSong(song.playlistId, song.songId)
            } else {
                target.upsertSong(song)
            }
        }
        batch.visiblePlaylistIds?.let(target::reconcileVisiblePlaylists)
        if (batch.playlists.isNotEmpty() || batch.songs.isNotEmpty() || batch.visiblePlaylistIds != null) target.refresh()
    }
}
