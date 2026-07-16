package com.metrolist.desktop.windows

import com.metrolist.desktop.player.DesktopMusicPlayer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SystemMediaSessionManager(
    private val player: DesktopMusicPlayer,
    backend: SystemMediaBackend? = JmtcSystemMediaBackend.createOrNull(),
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coordinator = backend?.let { mediaBackend ->
        SystemMediaCoordinator(
            backend = mediaBackend,
            controls = SystemMediaControls(
                playPause = { scope.launch { player.playPause() } },
                next = { scope.launch { player.skipNext() } },
                previous = { scope.launch { player.skipPrevious() } },
                seekTo = { seconds -> scope.launch { player.seekTo(seconds.toFloat()) } },
            ),
        )
    }
    private val started = AtomicBoolean(false)
    private var updateJob: Job? = null

    fun start() {
        val activeCoordinator = coordinator ?: return
        if (!started.compareAndSet(false, true)) return
        activeCoordinator.start()
        updateJob = scope.launch {
            while (isActive) {
                player.currentSong.value?.let { song ->
                    activeCoordinator.update(
                        SystemMediaSnapshot(
                            mediaId = song.item.videoId,
                            title = song.title,
                            artist = song.artist,
                            album = song.item.album,
                            artwork = song.thumbnailUrl,
                            playbackState = player.state.value,
                            positionSeconds = player.position.value,
                            durationSeconds = player.duration.value,
                            shuffle = player.shuffle.value,
                            repeatMode = player.repeatMode.value,
                        ),
                    )
                }
                delay(1_000)
            }
        }
    }

    override fun close() {
        updateJob?.cancel()
        updateJob = null
        coordinator?.close()
        scope.cancel()
    }
}
