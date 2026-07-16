package com.metrolist.desktop.discord

import com.metrolist.desktop.player.DesktopMusicPlayer
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal interface DiscordPresencePublisher : Closeable {
    fun publish(activity: DiscordRpcActivity): Boolean
    fun clear()
}

internal class DiscordPresenceCoordinator(
    private val publisher: DiscordPresencePublisher,
    private val nowMs: () -> Long = System::currentTimeMillis,
) {
    internal var lastPublished: DiscordPresenceAnchor? = null
        private set
    private var presenceVisible = false

    fun update(enabled: Boolean, snapshot: DiscordPresenceSnapshot) {
        if (!enabled) {
            clearIfVisible()
            return
        }

        val currentTime = nowMs()
        if (!DiscordPresencePolicy.shouldPublish(lastPublished, snapshot, currentTime)) return
        val activity = DiscordPresencePolicy.activity(snapshot, currentTime)
        if (activity == null) {
            clearIfVisible()
            lastPublished = DiscordPresenceAnchor(snapshot, currentTime)
            return
        }

        if (publisher.publish(activity)) {
            presenceVisible = true
            lastPublished = DiscordPresenceAnchor(snapshot, currentTime)
        }
    }

    fun close() {
        publisher.close()
        presenceVisible = false
        lastPublished = null
    }

    private fun clearIfVisible() {
        if (presenceVisible) publisher.clear()
        presenceVisible = false
        lastPublished = null
    }
}

internal class DiscordPresenceManager(
    private val player: DesktopMusicPlayer,
    private val enabled: StateFlow<Boolean>,
    publisher: DiscordPresencePublisher = DiscordRpcClient(APPLICATION_ID),
    private val pollIntervalMs: Long = POLL_INTERVAL_MS,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val coordinator = DiscordPresenceCoordinator(publisher)
    private var pollingJob: Job? = null

    fun start() {
        if (pollingJob != null) return
        pollingJob = scope.launch {
            while (isActive) {
                coordinator.update(enabled.value, player.discordPresenceSnapshot())
                delay(pollIntervalMs)
            }
        }
    }

    override fun close() {
        pollingJob?.cancel()
        pollingJob = null
        coordinator.close()
        scope.cancel()
    }

    private fun DesktopMusicPlayer.discordPresenceSnapshot(): DiscordPresenceSnapshot {
        val song = currentSong.value
        val playbackState = when (state.value) {
            DesktopMusicPlayer.State.PLAYING,
            DesktopMusicPlayer.State.BUFFERING,
            -> DiscordPlaybackState.PLAYING
            DesktopMusicPlayer.State.PAUSED -> DiscordPlaybackState.PAUSED
            DesktopMusicPlayer.State.IDLE,
            DesktopMusicPlayer.State.ERROR,
            -> DiscordPlaybackState.STOPPED
        }
        return DiscordPresenceSnapshot(
            videoId = song?.item?.videoId.orEmpty(),
            title = song?.title.orEmpty(),
            artist = song?.artist.orEmpty(),
            album = song?.item?.album,
            thumbnailUrl = song?.thumbnailUrl,
            playbackState = playbackState,
            positionSeconds = position.value,
            durationSeconds = duration.value,
        )
    }

    private companion object {
        const val APPLICATION_ID = "1527193618769510430"
        const val POLL_INTERVAL_MS = 1_000L
    }
}
