package com.metrolist.desktop.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RemotePlaylistApplicatorTest {
    @Test
    fun `remote batch applies playlist before songs and honors tombstones`() {
        val target = RecordingTarget()
        val batch = RemoteSyncBatch(
            playlists = listOf(
                remotePlaylist("active", deletedAt = null),
                remotePlaylist("deleted", deletedAt = 50L),
            ),
            songs = listOf(
                remoteSong("active", "song-active", deletedAt = null),
                remoteSong("active", "song-deleted", deletedAt = 70L),
                remoteSong("deleted", "song-stale", deletedAt = null),
            ),
        )

        RemotePlaylistApplicator(target).apply(batch)

        assertEquals(
            listOf(
                "upsert-playlist:active",
                "delete-playlist:deleted",
                "upsert-song:active:song-active",
                "delete-song:active:song-deleted",
                "delete-song:deleted:song-stale",
                "refresh",
            ),
            target.events,
        )
    }

    @Test
    fun `full visibility snapshot removes playlists no longer shared with this account`() {
        val target = RecordingTarget()

        RemotePlaylistApplicator(target).apply(
            RemoteSyncBatch(visiblePlaylistIds = setOf("still-visible")),
        )

        assertEquals(listOf("reconcile:still-visible", "refresh"), target.events)
    }

    private fun remotePlaylist(id: String, deletedAt: Long?) = RemotePlaylist(
        id = id,
        name = id,
        description = "",
        coverUrl = null,
        ownerId = "owner",
        deletedAt = deletedAt,
        changeSequence = 1L,
    )

    private fun remoteSong(playlistId: String, songId: String, deletedAt: Long?) = RemotePlaylistSong(
        playlistId = playlistId,
        songId = songId,
        title = songId,
        artist = "Artist",
        album = null,
        thumbnailUrl = null,
        duration = 100,
        position = 0,
        addedBy = "owner",
        addedByName = "allan",
        addedAtEpochSeconds = 10L,
        deletedAt = deletedAt,
        changeSequence = 1L,
    )

    private class RecordingTarget : LocalPlaylistSyncTarget {
        val events = mutableListOf<String>()

        override fun upsertPlaylist(playlist: RemotePlaylist) {
            events += "upsert-playlist:${playlist.id}"
        }

        override fun deletePlaylist(playlistId: String) {
            events += "delete-playlist:$playlistId"
        }

        override fun upsertSong(song: RemotePlaylistSong) {
            events += "upsert-song:${song.playlistId}:${song.songId}"
        }

        override fun deleteSong(playlistId: String, songId: String) {
            events += "delete-song:$playlistId:$songId"
        }

        override fun refresh() {
            events += "refresh"
        }

        override fun reconcileVisiblePlaylists(visiblePlaylistIds: Set<String>) {
            events += "reconcile:${visiblePlaylistIds.sorted().joinToString()}"
        }
    }
}
