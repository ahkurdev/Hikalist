package com.metrolist.desktop.ui

import com.metrolist.desktop.offline.OfflineDownloadRequest
import com.metrolist.desktop.offline.OfflineDownloadState
import com.metrolist.desktop.offline.OfflineSongMetadata
import com.metrolist.desktop.player.DesktopMusicPlayer

data class PlaylistDownloadSummary(
    val completed: Int,
    val total: Int,
    val active: Boolean,
) {
    val allDownloaded: Boolean = total > 0 && completed == total
}

fun playlistDownloadSummary(
    videoIds: List<String>,
    states: Map<String, OfflineDownloadState>,
    isDownloaded: (String) -> Boolean,
): PlaylistDownloadSummary {
    val uniqueIds = videoIds.distinct()
    return PlaylistDownloadSummary(
        completed = uniqueIds.count(isDownloaded),
        total = uniqueIds.size,
        active = uniqueIds.any { states[it] is OfflineDownloadState.Queued || states[it] is OfflineDownloadState.Downloading },
    )
}

fun DesktopMusicPlayer.QueueItem.toOfflineDownloadRequest() = OfflineDownloadRequest(
    metadata = OfflineSongMetadata(
        videoId = videoId,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        durationSeconds = duration,
    ),
    playlistId = playlistId,
)
