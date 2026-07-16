package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopMusicPlayerPreferencesTest {
    @Test
    fun `player starts with persisted volume and repeat preferences`() {
        val player = DesktopMusicPlayer(
            ffmpegPathProvider = { null },
            initialVolumePercent = 61,
            initialRepeatMode = RepeatMode.ONE,
        )

        assertEquals(61, player.volumePercent.value)
        assertEquals(RepeatMode.ONE, player.repeatMode.value)

        player.dispose()
    }

    @Test
    fun `volume and repeat settings update live player state`() {
        val player = DesktopMusicPlayer(ffmpegPathProvider = { null })

        player.setVolume(42)
        player.setRepeatMode(RepeatMode.OFF)

        assertEquals(42, player.volumePercent.value)
        assertEquals(RepeatMode.OFF, player.repeatMode.value)

        player.dispose()
    }
}
