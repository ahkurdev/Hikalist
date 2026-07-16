package com.metrolist.desktop.player

import com.metrolist.desktop.preferences.AudioQualityPreference
import kotlin.math.abs

internal object AudioQualityPolicy {
    fun <T> select(
        candidates: List<T>,
        quality: AudioQualityPreference,
        bitrateOf: (T) -> Int,
    ): T? {
        val validCandidates = candidates.filter { bitrateOf(it) > 0 }
        return when (quality) {
            AudioQualityPreference.AUTO -> validCandidates.minWithOrNull(
                compareBy<T> { abs(bitrateOf(it) - AUTO_TARGET_BITRATE) }
                    .thenByDescending(bitrateOf),
            )
            AudioQualityPreference.HIGH -> validCandidates.maxByOrNull(bitrateOf)
            AudioQualityPreference.DATA_SAVER -> validCandidates.minByOrNull(bitrateOf)
        }
    }

    private const val AUTO_TARGET_BITRATE = 128_000
}
