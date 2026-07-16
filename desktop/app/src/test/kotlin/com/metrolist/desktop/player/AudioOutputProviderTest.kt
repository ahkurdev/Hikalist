package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputProviderTest {
    @Test
    fun `stored device falls back to system default when it disappears`() {
        val devices = listOf(
            AudioOutputDevice(AudioOutputDevice.SYSTEM_DEFAULT_ID, "System default"),
            AudioOutputDevice("headset", "USB Headset"),
        )

        assertEquals("headset", AudioOutputSelection.resolve("headset", devices))
        assertEquals(AudioOutputDevice.SYSTEM_DEFAULT_ID, AudioOutputSelection.resolve("missing", devices))
    }

    @Test
    fun `device ids are stable and distinguish equal display names`() {
        val first = AudioOutputDevice.stableId("Speakers", "Vendor A", "Primary", "1")
        val same = AudioOutputDevice.stableId("Speakers", "Vendor A", "Primary", "1")
        val second = AudioOutputDevice.stableId("Speakers", "Vendor B", "Primary", "1")

        assertEquals(first, same)
        assertTrue(first != second)
    }

    @Test
    fun `output change reconnects only active playback`() {
        assertTrue(AudioOutputChangePolicy.shouldReconnect(DesktopMusicPlayer.State.PLAYING, "a", "b"))
        assertTrue(AudioOutputChangePolicy.shouldReconnect(DesktopMusicPlayer.State.BUFFERING, "a", "b"))
        assertFalse(AudioOutputChangePolicy.shouldReconnect(DesktopMusicPlayer.State.PAUSED, "a", "b"))
        assertFalse(AudioOutputChangePolicy.shouldReconnect(DesktopMusicPlayer.State.PLAYING, "a", "a"))
    }
}
