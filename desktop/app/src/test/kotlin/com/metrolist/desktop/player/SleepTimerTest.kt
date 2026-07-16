package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerTest {
    @Test
    fun `preset durations are expressed in seconds`() {
        assertEquals(15 * 60L, SleepTimerOption.MINUTES_15.durationSeconds)
        assertEquals(30 * 60L, SleepTimerOption.MINUTES_30.durationSeconds)
        assertEquals(45 * 60L, SleepTimerOption.MINUTES_45.durationSeconds)
        assertEquals(60 * 60L, SleepTimerOption.MINUTES_60.durationSeconds)
        assertEquals(null, SleepTimerOption.END_OF_TRACK.durationSeconds)
    }

    @Test
    fun `end of track latch is consumed only once`() {
        val latch = EndOfTrackSleepLatch()
        latch.enabled = true

        assertTrue(latch.consume())
        assertFalse(latch.consume())
    }

    @Test
    fun `disabled end of track latch does not stop playback`() {
        assertFalse(EndOfTrackSleepLatch().consume())
    }
}
