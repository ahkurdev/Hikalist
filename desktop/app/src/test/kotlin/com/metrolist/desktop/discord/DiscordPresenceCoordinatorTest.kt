package com.metrolist.desktop.discord

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiscordPresenceCoordinatorTest {
    @Test
    fun `playing pause seek and stop are published without position spam`() {
        val publisher = RecordingPublisher()
        var now = 100_000L
        val coordinator = DiscordPresenceCoordinator(publisher) { now }

        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PLAYING, 30.0))
        now += 1_000L
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PLAYING, 31.0))
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PAUSED, 31.0))
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PLAYING, 180.0))
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.STOPPED, 0.0))
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.STOPPED, 0.0))

        assertEquals(3, publisher.activities.size)
        assertEquals("Ada", publisher.activities.first().details)
        assertEquals("Paused \u2022 Lyodra, Afgan", publisher.activities[1].state)
        assertEquals(1, publisher.clearCount)
    }

    @Test
    fun `disabling clears once and enabling publishes current song again`() {
        val publisher = RecordingPublisher()
        val coordinator = DiscordPresenceCoordinator(publisher) { 100_000L }
        val playing = snapshot(DiscordPlaybackState.PLAYING, 30.0)

        coordinator.update(enabled = true, snapshot = playing)
        coordinator.update(enabled = false, snapshot = playing)
        coordinator.update(enabled = false, snapshot = playing)
        coordinator.update(enabled = true, snapshot = playing)

        assertEquals(2, publisher.activities.size)
        assertEquals(1, publisher.clearCount)
    }

    @Test
    fun `failed publish remains eligible for retry`() {
        val publisher = RecordingPublisher(publishSuccess = false)
        val coordinator = DiscordPresenceCoordinator(publisher) { 100_000L }

        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PLAYING, 30.0))
        coordinator.update(enabled = true, snapshot = snapshot(DiscordPlaybackState.PLAYING, 30.0))

        assertEquals(2, publisher.activities.size)
        assertNull(coordinator.lastPublished)
    }

    private fun snapshot(state: DiscordPlaybackState, position: Double) = DiscordPresenceSnapshot(
        videoId = "video-id",
        title = "Ada",
        artist = "Lyodra, Afgan",
        album = "Ada",
        thumbnailUrl = "https://example.com/cover.jpg",
        playbackState = state,
        positionSeconds = position,
        durationSeconds = 240.0,
    )

    private class RecordingPublisher(
        private val publishSuccess: Boolean = true,
    ) : DiscordPresencePublisher {
        val activities = mutableListOf<DiscordRpcActivity>()
        var clearCount = 0

        override fun publish(activity: DiscordRpcActivity): Boolean {
            activities += activity
            return publishSuccess
        }

        override fun clear() {
            clearCount++
        }

        override fun close() = Unit
    }
}
