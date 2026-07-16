package com.metrolist.desktop.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {
    @Test
    fun `new settings use safe desktop defaults`() {
        val settings = AppSettings(InMemorySettingsStorage())

        assertEquals(ThemePreference.SYSTEM, settings.theme.value)
        assertEquals(AudioQualityPreference.AUTO, settings.audioQuality.value)
        assertEquals(85, settings.volumePercent.value)
        assertTrue(settings.showLyricsByDefault.value)
        assertTrue(settings.discordRichPresence.value)
        assertEquals(RepeatPreference.ALL, settings.repeat.value)
        assertFalse(settings.equalizerEnabled.value)
        assertEquals("FLAT", settings.equalizerPreset.value)
        assertEquals(List(10) { 0f }, settings.equalizerGainsDb.value)
        assertEquals("system-default", settings.audioOutputId.value)
    }

    @Test
    fun `changed settings survive a store restart`() {
        val storage = InMemorySettingsStorage()
        AppSettings(storage).apply {
            setTheme(ThemePreference.LIGHT)
            setAudioQuality(AudioQualityPreference.DATA_SAVER)
            setVolumePercent(160)
            setShowLyricsByDefault(false)
            setDiscordRichPresence(false)
            setRepeat(RepeatPreference.ONE)
            setEqualizerEnabled(true)
            setEqualizer("ROCK", listOf(4f, 3f, 2f, 0f, -1f, 1f, 3f, 4f, 5f, 4f))
            setAudioOutputId("usb-headset")
        }

        val restored = AppSettings(storage)

        assertEquals(ThemePreference.LIGHT, restored.theme.value)
        assertEquals(AudioQualityPreference.DATA_SAVER, restored.audioQuality.value)
        assertEquals(100, restored.volumePercent.value)
        assertFalse(restored.showLyricsByDefault.value)
        assertFalse(restored.discordRichPresence.value)
        assertEquals(RepeatPreference.ONE, restored.repeat.value)
        assertTrue(restored.equalizerEnabled.value)
        assertEquals("ROCK", restored.equalizerPreset.value)
        assertEquals(listOf(4f, 3f, 2f, 0f, -1f, 1f, 3f, 4f, 5f, 4f), restored.equalizerGainsDb.value)
        assertEquals("usb-headset", restored.audioOutputId.value)
    }

    @Test
    fun `unknown enum values fall back without overwriting other settings`() {
        val storage = InMemorySettingsStorage(
            strings = mutableMapOf(
                "theme" to "NEON",
                "audio_quality" to "ULTRA",
                "repeat" to "FOREVER",
            ),
            ints = mutableMapOf("volume_percent" to 62),
        )

        val settings = AppSettings(storage)

        assertEquals(ThemePreference.SYSTEM, settings.theme.value)
        assertEquals(AudioQualityPreference.AUTO, settings.audioQuality.value)
        assertEquals(RepeatPreference.ALL, settings.repeat.value)
        assertEquals(62, settings.volumePercent.value)
    }

    @Test
    fun `theme preference resolves system dark mode explicitly`() {
        assertTrue(ThemePreference.SYSTEM.useDarkTheme(systemDark = true))
        assertFalse(ThemePreference.SYSTEM.useDarkTheme(systemDark = false))
        assertTrue(ThemePreference.DARK.useDarkTheme(systemDark = false))
        assertFalse(ThemePreference.LIGHT.useDarkTheme(systemDark = true))
    }
}

private class InMemorySettingsStorage(
    private val strings: MutableMap<String, String> = mutableMapOf(),
    private val ints: MutableMap<String, Int> = mutableMapOf(),
    private val booleans: MutableMap<String, Boolean> = mutableMapOf(),
) : SettingsStorage {
    override fun getString(key: String, default: String): String = strings[key] ?: default

    override fun getInt(key: String, default: Int): Int = ints[key] ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean = booleans[key] ?: default

    override fun putString(key: String, value: String) {
        strings[key] = value
    }

    override fun putInt(key: String, value: Int) {
        ints[key] = value
    }

    override fun putBoolean(key: String, value: Boolean) {
        booleans[key] = value
    }
}
