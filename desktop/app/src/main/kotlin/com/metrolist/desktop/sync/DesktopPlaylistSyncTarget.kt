package com.metrolist.desktop.sync

import com.metrolist.desktop.db.repository.DataRepository
import com.metrolist.desktop.db.repository.SongRow
import com.metrolist.desktop.playlist.PlaylistMetadataStore
import com.metrolist.desktop.playlist.PlaylistSongMetadataStore

class DesktopPlaylistSyncTarget(
    private val repository: DataRepository,
    private val metadataStore: PlaylistMetadataStore,
    private val songMetadataStore: PlaylistSongMetadataStore,
    private val onChanged: () -> Unit,
) : LocalPlaylistSyncTarget {
    override fun upsertPlaylist(playlist: RemotePlaylist) {
        if (playlist.id == LikedSongsSync.LOCAL_PLAYLIST_ID) return
        repository.upsertSyncedPlaylist(playlist.id, playlist.name, playlist.coverUrl)
        metadataStore.setDescription(playlist.id, playlist.description)
    }

    override fun deletePlaylist(playlistId: String) {
        if (playlistId == LikedSongsSync.LOCAL_PLAYLIST_ID) {
            repository.getLikedSongs().forEach { repository.setSongLiked(it.id, false) }
            songMetadataStore.deletePlaylist(playlistId)
            return
        }
        repository.deletePlaylist(playlistId)
        metadataStore.delete(playlistId)
        songMetadataStore.deletePlaylist(playlistId)
    }

    override fun upsertSong(song: RemotePlaylistSong) {
        repository.saveSong(
            SongRow(
                id = song.songId,
                title = song.title,
                duration = song.duration,
                thumbnailUrl = song.thumbnailUrl,
                albumName = song.album,
                artists = song.artist.split(',').map(String::trim).filter(String::isNotBlank),
                album = song.album,
            ),
        )
        if (song.playlistId == LikedSongsSync.LOCAL_PLAYLIST_ID) {
            repository.setSongLiked(song.songId, true)
            songMetadataStore.upsert(
                playlistId = song.playlistId,
                songId = song.songId,
                addedBy = song.addedByName,
                addedAtEpochSeconds = song.addedAtEpochSeconds,
            )
            return
        }
        repository.addSongToPlaylist(song.playlistId, song.songId)
        repository.setPlaylistSongPosition(song.playlistId, song.songId, song.position)
        songMetadataStore.upsert(
            playlistId = song.playlistId,
            songId = song.songId,
            addedBy = song.addedByName,
            addedAtEpochSeconds = song.addedAtEpochSeconds,
        )
    }

    override fun deleteSong(playlistId: String, songId: String) {
        if (playlistId == LikedSongsSync.LOCAL_PLAYLIST_ID) {
            repository.setSongLiked(songId, false)
            songMetadataStore.remove(playlistId, songId)
            return
        }
        repository.removeSongFromPlaylist(playlistId, songId)
        songMetadataStore.remove(playlistId, songId)
    }

    override fun reconcileVisiblePlaylists(visiblePlaylistIds: Set<String>) {
        repository.getSyncedPlaylistIds()
            .filterNot(visiblePlaylistIds::contains)
            .forEach(::deletePlaylist)
    }

    override fun refresh() = onChanged()
}
