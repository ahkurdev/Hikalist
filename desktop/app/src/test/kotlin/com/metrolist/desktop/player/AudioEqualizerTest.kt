package com.metrolist.desktop.player

import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioEqualizerTest {
    @Test
    fun `disabled equalizer leaves pcm untouched`() {
        val equalizer = AudioEqualizer(sampleRate = 44_100, channelCount = 2)
        equalizer.update(EqualizerConfiguration(enabled = false))
        val source = stereoSine(frequency = 1_000.0, frames = 2_048, amplitude = 8_000.0)

        assertArrayEquals(source, equalizer.process(source.copyOf()))
    }

    @Test
    fun `boosting one kilohertz increases its rms level`() {
        val equalizer = AudioEqualizer(sampleRate = 44_100, channelCount = 2)
        val source = stereoSine(frequency = 1_000.0, frames = 8_192, amplitude = 1_000.0)
        val before = rms(source)
        equalizer.update(
            EqualizerConfiguration(
                enabled = true,
                gainsDb = EqualizerBand.entries.associateWith { band ->
                    if (band == EqualizerBand.HZ_1K) 9f else 0f
                },
            ),
        )

        val after = rms(equalizer.process(source.copyOf()))

        assertTrue("Expected boosted RMS ($after) to exceed source RMS ($before)", after > before * 1.25)
    }

    @Test
    fun `preset exposes all ten bounded bands`() {
        EqualizerPreset.entries.forEach { preset ->
            assertEquals(EqualizerBand.entries.size, preset.gainsDb.size)
            assertTrue(preset.gainsDb.values.all { it in -12f..12f })
        }
    }

    private fun stereoSine(frequency: Double, frames: Int, amplitude: Double): ByteArray {
        val bytes = ByteArray(frames * 4)
        repeat(frames) { frame ->
            val sample = (sin(2.0 * PI * frequency * frame / 44_100.0) * amplitude).toInt().toShort()
            repeat(2) { channel ->
                val offset = frame * 4 + channel * 2
                bytes[offset] = (sample.toInt() and 0xff).toByte()
                bytes[offset + 1] = ((sample.toInt() ushr 8) and 0xff).toByte()
            }
        }
        return bytes
    }

    private fun rms(bytes: ByteArray): Double {
        var sum = 0.0
        var samples = 0
        var index = 0
        while (index + 1 < bytes.size) {
            val sample = ((bytes[index].toInt() and 0xff) or (bytes[index + 1].toInt() shl 8)).toShort().toDouble()
            sum += sample * sample
            samples++
            index += 2
        }
        return sqrt(sum / samples)
    }
}
