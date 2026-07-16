package com.metrolist.desktop.sync

object LikedSongsSync {
    const val LOCAL_PLAYLIST_ID = "LIKED_SONGS"
    private const val REMOTE_PREFIX = "LIKED_SONGS:"

    fun remotePlaylistId(userId: String): String = "$REMOTE_PREFIX$userId"

    fun toRemotePlaylistId(localPlaylistId: String, userId: String): String =
        if (localPlaylistId == LOCAL_PLAYLIST_ID) remotePlaylistId(userId) else localPlaylistId

    fun toLocalPlaylistId(remotePlaylistId: String, userId: String): String =
        if (remotePlaylistId == remotePlaylistId(userId)) LOCAL_PLAYLIST_ID else remotePlaylistId
}
