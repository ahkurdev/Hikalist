package com.metrolist.desktop.player

import com.metrolist.desktop.preferences.AudioQualityPreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioQualityPolicyTest {
    private data class Candidate(val bitrate: Int)

    private val candidates = listOf(
        Candidate(64_000),
        Candidate(128_000),
        Candidate(256_000),
    )

    @Test
    fun `auto chooses the bitrate nearest 128 kbps`() {
        val selected = AudioQualityPolicy.select(candidates, AudioQualityPreference.AUTO, Candidate::bitrate)

        assertEquals(128_000, selected?.bitrate)
    }

    @Test
    fun `high chooses the highest valid bitrate`() {
        val selected = AudioQualityPolicy.select(candidates, AudioQualityPreference.HIGH, Candidate::bitrate)

        assertEquals(256_000, selected?.bitrate)
    }

    @Test
    fun `data saver chooses the lowest valid bitrate`() {
        val selected = AudioQualityPolicy.select(candidates, AudioQualityPreference.DATA_SAVER, Candidate::bitrate)

        assertEquals(64_000, selected?.bitrate)
    }

    @Test
    fun `invalid bitrates are ignored and empty candidates return null`() {
        val selected = AudioQualityPolicy.select(
            candidates = listOf(Candidate(0), Candidate(-1), Candidate(96_000)),
            quality = AudioQualityPreference.HIGH,
            bitrateOf = Candidate::bitrate,
        )

        assertEquals(96_000, selected?.bitrate)
        assertNull(AudioQualityPolicy.select(emptyList<Candidate>(), AudioQualityPreference.AUTO, Candidate::bitrate))
    }
}
