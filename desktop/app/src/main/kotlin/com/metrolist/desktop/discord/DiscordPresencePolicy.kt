package com.metrolist.desktop.discord

import kotlin.math.abs

enum class DiscordPlaybackState { PLAYING, PAUSED, STOPPED }

data class DiscordPresenceSnapshot(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val playbackState: DiscordPlaybackState,
    val positionSeconds: Double,
    val durationSeconds: Double,
)

data class DiscordPresenceAnchor(
    val snapshot: DiscordPresenceSnapshot,
    val publishedAtMs: Long,
)

object DiscordPresencePolicy {
    fun activity(snapshot: DiscordPresenceSnapshot, nowMs: Long): DiscordRpcActivity? {
        if (snapshot.playbackState == DiscordPlaybackState.STOPPED || snapshot.videoId.isBlank()) return null
        val playing = snapshot.playbackState == DiscordPlaybackState.PLAYING
        val positionMs = (snapshot.positionSeconds.coerceAtLeast(0.0) * 1_000).toLong()
        val startMs = (nowMs - positionMs).takeIf { playing }
        val endMs = if (playing && snapshot.durationSeconds > snapshot.positionSeconds) {
            startMs?.plus((snapshot.durationSeconds * 1_000).toLong())
        } else {
            null
        }
        val artist = snapshot.artist.ifBlank { "Unknown artist" }
        return DiscordRpcActivity(
            details = snapshot.title.ifBlank { "Unknown song" },
            state = if (playing) artist else "Paused \u2022 $artist",
            startTimestampMs = startMs,
            endTimestampMs = endMs,
            largeImage = snapshot.thumbnailUrl?.takeIf(::isPublicImageUrl),
            largeText = snapshot.album?.takeIf(String::isNotBlank) ?: snapshot.title,
            buttonLabel = "Listen on YouTube Music",
            buttonUrl = "https://music.youtube.com/watch?v=${snapshot.videoId}",
        )
    }

    fun shouldPublish(
        previous: DiscordPresenceAnchor?,
        current: DiscordPresenceSnapshot,
        nowMs: Long,
    ): Boolean {
        previous ?: return true
        val old = previous.snapshot
        if (
            old.videoId != current.videoId ||
            old.playbackState != current.playbackState ||
            old.durationSeconds != current.durationSeconds ||
            old.title != current.title ||
            old.artist != current.artist
        ) {
            return true
        }
        val elapsedSeconds = ((nowMs - previous.publishedAtMs).coerceAtLeast(0L) / 1_000.0)
        val expectedPosition = old.positionSeconds + if (old.playbackState == DiscordPlaybackState.PLAYING) elapsedSeconds else 0.0
        return abs(current.positionSeconds - expectedPosition) > SEEK_TOLERANCE_SECONDS ||
            nowMs - previous.publishedAtMs >= PERIODIC_REFRESH_MS
    }

    private fun isPublicImageUrl(value: String): Boolean =
        value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true)

    private const val SEEK_TOLERANCE_SECONDS = 2.5
    private const val PERIODIC_REFRESH_MS = 60_000L
}
