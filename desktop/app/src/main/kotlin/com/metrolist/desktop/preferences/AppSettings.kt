package com.metrolist.desktop.preferences

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreference { SYSTEM, DARK, LIGHT }

enum class AudioQualityPreference { AUTO, HIGH, DATA_SAVER }

enum class RepeatPreference { OFF, ALL, ONE }

fun ThemePreference.useDarkTheme(systemDark: Boolean): Boolean = when (this) {
    ThemePreference.SYSTEM -> systemDark
    ThemePreference.DARK -> true
    ThemePreference.LIGHT -> false
}

interface SettingsStorage {
    fun getString(key: String, default: String): String
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putString(key: String, value: String)
    fun putInt(key: String, value: Int)
    fun putBoolean(key: String, value: Boolean)
}

class AppSettings(
    private val storage: SettingsStorage = DesktopSettingsStorage,
) {
    private val _theme = MutableStateFlow(readEnum(THEME_KEY, ThemePreference.SYSTEM))
    val theme: StateFlow<ThemePreference> = _theme.asStateFlow()

    private val _audioQuality = MutableStateFlow(readEnum(AUDIO_QUALITY_KEY, AudioQualityPreference.AUTO))
    val audioQuality: StateFlow<AudioQualityPreference> = _audioQuality.asStateFlow()

    private val _volumePercent = MutableStateFlow(storage.getInt(VOLUME_KEY, DEFAULT_VOLUME).coerceIn(0, 100))
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()

    private val _showLyricsByDefault = MutableStateFlow(storage.getBoolean(SHOW_LYRICS_KEY, true))
    val showLyricsByDefault: StateFlow<Boolean> = _showLyricsByDefault.asStateFlow()

    private val _discordRichPresence = MutableStateFlow(storage.getBoolean(DISCORD_RICH_PRESENCE_KEY, true))
    val discordRichPresence: StateFlow<Boolean> = _discordRichPresence.asStateFlow()

    private val _repeat = MutableStateFlow(readEnum(REPEAT_KEY, RepeatPreference.ALL))
    val repeat: StateFlow<RepeatPreference> = _repeat.asStateFlow()

    private val _equalizerEnabled = MutableStateFlow(storage.getBoolean(EQUALIZER_ENABLED_KEY, false))
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _equalizerPreset = MutableStateFlow(storage.getString(EQUALIZER_PRESET_KEY, "FLAT"))
    val equalizerPreset: StateFlow<String> = _equalizerPreset.asStateFlow()

    private val _equalizerGainsDb = MutableStateFlow(readEqualizerGains())
    val equalizerGainsDb: StateFlow<List<Float>> = _equalizerGainsDb.asStateFlow()

    private val _audioOutputId = MutableStateFlow(storage.getString(AUDIO_OUTPUT_KEY, "system-default"))
    val audioOutputId: StateFlow<String> = _audioOutputId.asStateFlow()

    fun setTheme(value: ThemePreference) {
        storage.putString(THEME_KEY, value.name)
        _theme.value = value
    }

    fun setAudioQuality(value: AudioQualityPreference) {
        storage.putString(AUDIO_QUALITY_KEY, value.name)
        _audioQuality.value = value
    }

    fun setVolumePercent(value: Int) {
        val safeValue = value.coerceIn(0, 100)
        storage.putInt(VOLUME_KEY, safeValue)
        _volumePercent.value = safeValue
    }

    fun setShowLyricsByDefault(value: Boolean) {
        storage.putBoolean(SHOW_LYRICS_KEY, value)
        _showLyricsByDefault.value = value
    }

    fun setDiscordRichPresence(value: Boolean) {
        storage.putBoolean(DISCORD_RICH_PRESENCE_KEY, value)
        _discordRichPresence.value = value
    }

    fun setRepeat(value: RepeatPreference) {
        storage.putString(REPEAT_KEY, value.name)
        _repeat.value = value
    }

    fun setEqualizerEnabled(value: Boolean) {
        storage.putBoolean(EQUALIZER_ENABLED_KEY, value)
        _equalizerEnabled.value = value
    }

    fun setEqualizer(preset: String, gainsDb: List<Float>) {
        val safeGains = gainsDb.take(EQUALIZER_BAND_COUNT).map { it.coerceIn(-12f, 12f) }
            .let { it + List(EQUALIZER_BAND_COUNT - it.size) { 0f } }
        storage.putString(EQUALIZER_PRESET_KEY, preset)
        storage.putString(EQUALIZER_GAINS_KEY, safeGains.joinToString(","))
        _equalizerPreset.value = preset
        _equalizerGainsDb.value = safeGains
    }

    fun setAudioOutputId(value: String) {
        storage.putString(AUDIO_OUTPUT_KEY, value)
        _audioOutputId.value = value
    }

    private fun readEqualizerGains(): List<Float> {
        val parsed = storage.getString(EQUALIZER_GAINS_KEY, "")
            .split(',')
            .mapNotNull(String::toFloatOrNull)
            .take(EQUALIZER_BAND_COUNT)
            .map { it.coerceIn(-12f, 12f) }
        return parsed + List(EQUALIZER_BAND_COUNT - parsed.size) { 0f }
    }

    private inline fun <reified T : Enum<T>> readEnum(key: String, default: T): T {
        val stored = storage.getString(key, default.name)
        return enumValues<T>().firstOrNull { it.name == stored } ?: default
    }

    private companion object {
        const val THEME_KEY = "theme"
        const val AUDIO_QUALITY_KEY = "audio_quality"
        const val VOLUME_KEY = "volume_percent"
        const val SHOW_LYRICS_KEY = "show_lyrics_by_default"
        const val DISCORD_RICH_PRESENCE_KEY = "discord_rich_presence"
        const val REPEAT_KEY = "repeat"
        const val EQUALIZER_ENABLED_KEY = "equalizer_enabled"
        const val EQUALIZER_PRESET_KEY = "equalizer_preset"
        const val EQUALIZER_GAINS_KEY = "equalizer_gains_db"
        const val EQUALIZER_BAND_COUNT = 10
        const val AUDIO_OUTPUT_KEY = "audio_output_id"
        const val DEFAULT_VOLUME = 85
    }
}

private object DesktopSettingsStorage : SettingsStorage {
    override fun getString(key: String, default: String): String =
        DesktopPreferences.getString(key, default) ?: default

    override fun getInt(key: String, default: Int): Int = DesktopPreferences.getInt(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean = DesktopPreferences.getBoolean(key, default)

    override fun putString(key: String, value: String) = DesktopPreferences.putString(key, value)

    override fun putInt(key: String, value: Int) = DesktopPreferences.putInt(key, value)

    override fun putBoolean(key: String, value: Boolean) = DesktopPreferences.putBoolean(key, value)
}
