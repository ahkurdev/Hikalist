package com.metrolist.desktop.discord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordPresencePolicyTest {
    @Test
    fun `playing song produces listening activity with synchronized timer`() {
        val activity = DiscordPresencePolicy.activity(
            snapshot(playbackState = DiscordPlaybackState.PLAYING, positionSeconds = 30.0, durationSeconds = 240.0),
            nowMs = 100_000,
        )

        requireNotNull(activity)
        assertEquals("Ada", activity.details)
        assertEquals("Lyodra, Afgan", activity.state)
        assertEquals(70_000L, activity.startTimestampMs)
        assertEquals(310_000L, activity.endTimestampMs)
        assertEquals("https://example.com/cover.jpg", activity.largeImage)
        assertEquals("https://music.youtube.com/watch?v=video-id", activity.buttonUrl)
    }

    @Test
    fun `paused song remains visible without a running timer`() {
        val activity = DiscordPresencePolicy.activity(
            snapshot(playbackState = DiscordPlaybackState.PAUSED, positionSeconds = 80.0),
            nowMs = 100_000,
        )

        requireNotNull(activity)
        assertEquals("Paused \u2022 Lyodra, Afgan", activity.state)
        assertNull(activity.startTimestampMs)
        assertNull(activity.endTimestampMs)
    }

    @Test
    fun `stopped playback clears presence`() {
        assertNull(
            DiscordPresencePolicy.activity(
                snapshot(playbackState = DiscordPlaybackState.STOPPED),
                nowMs = 100_000,
            ),
        )
    }

    @Test
    fun `normal playback drift does not spam rpc but seek does`() {
        val previous = DiscordPresenceAnchor(
            snapshot = snapshot(playbackState = DiscordPlaybackState.PLAYING, positionSeconds = 30.0),
            publishedAtMs = 100_000,
        )

        assertFalse(
            DiscordPresencePolicy.shouldPublish(
                previous,
                snapshot(playbackState = DiscordPlaybackState.PLAYING, positionSeconds = 31.0),
                nowMs = 101_000,
            ),
        )
        assertTrue(
            DiscordPresencePolicy.shouldPublish(
                previous,
                snapshot(playbackState = DiscordPlaybackState.PLAYING, positionSeconds = 180.0),
                nowMs = 101_000,
            ),
        )
    }

    private fun snapshot(
        playbackState: DiscordPlaybackState,
        positionSeconds: Double = 0.0,
        durationSeconds: Double = 240.0,
    ) = DiscordPresenceSnapshot(
        videoId = "video-id",
        title = "Ada",
        artist = "Lyodra, Afgan",
        album = "Ada",
        thumbnailUrl = "https://example.com/cover.jpg",
        playbackState = playbackState,
        positionSeconds = positionSeconds,
        durationSeconds = durationSeconds,
    )
}
