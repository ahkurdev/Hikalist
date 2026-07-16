package com.metrolist.desktop.player

import com.metrolist.desktop.offline.OfflineMedia
import java.io.File

sealed interface PlaybackSource {
    val durationSeconds: Int

    data class Local(
        val audioFile: File,
        val coverFile: File?,
        override val durationSeconds: Int,
    ) : PlaybackSource

    data class Online(
        val stream: DesktopYTPlayerUtils.PlaybackData,
    ) : PlaybackSource {
        override val durationSeconds: Int = stream.durationSeconds
    }
}

class PlaybackSourceSelector(
    private val offlineMediaProvider: (videoId: String) -> OfflineMedia?,
    private val onlineResolver: suspend (DesktopMusicPlayer.QueueItem) -> DesktopYTPlayerUtils.PlaybackData,
) {
    suspend fun resolve(item: DesktopMusicPlayer.QueueItem): PlaybackSource {
        offlineMediaProvider(item.videoId)?.let { media ->
            return PlaybackSource.Local(
                audioFile = media.audioFile,
                coverFile = media.coverFile,
                durationSeconds = media.metadata.durationSeconds.takeIf { it > 0 } ?: item.duration,
            )
        }
        return PlaybackSource.Online(onlineResolver(item))
    }
}
