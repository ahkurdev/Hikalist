package com.metrolist.desktop.player

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

enum class EqualizerBand(val frequencyHz: Double, val label: String) {
    HZ_31(31.0, "31"),
    HZ_62(62.0, "62"),
    HZ_125(125.0, "125"),
    HZ_250(250.0, "250"),
    HZ_500(500.0, "500"),
    HZ_1K(1_000.0, "1K"),
    HZ_2K(2_000.0, "2K"),
    HZ_4K(4_000.0, "4K"),
    HZ_8K(8_000.0, "8K"),
    HZ_16K(16_000.0, "16K"),
}

enum class EqualizerPreset(val displayName: String, values: List<Float>) {
    FLAT("Flat", listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)),
    BASS_BOOST("Bass Boost", listOf(7f, 6f, 5f, 3f, 1f, 0f, -1f, -2f, -2f, -2f)),
    TREBLE_BOOST("Treble Boost", listOf(-2f, -2f, -2f, -1f, 0f, 1f, 3f, 5f, 6f, 7f)),
    VOCAL("Vocal", listOf(-3f, -2f, -1f, 0f, 2f, 4f, 5f, 4f, 2f, 0f)),
    ROCK("Rock", listOf(5f, 4f, 3f, 1f, -1f, 1f, 3f, 5f, 5f, 4f)),
    POP("Pop", listOf(-1f, 1f, 3f, 4f, 3f, 1f, -1f, -1f, 1f, 2f)),
    CUSTOM("Custom", List(EqualizerBand.entries.size) { 0f }),
    ;

    val gainsDb: Map<EqualizerBand, Float> = EqualizerBand.entries.zip(values).toMap()

    companion object {
        fun fromStored(value: String): EqualizerPreset = entries.firstOrNull { it.name == value } ?: FLAT
    }
}

data class EqualizerConfiguration(
    val enabled: Boolean = false,
    val gainsDb: Map<EqualizerBand, Float> = EqualizerPreset.FLAT.gainsDb,
) {
    fun gainFor(band: EqualizerBand): Float = gainsDb[band]?.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB) ?: 0f

    companion object {
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
    }
}

class AudioEqualizer(
    private val sampleRate: Int,
    private val channelCount: Int,
) {
    @Volatile
    private var processingChain: ProcessingChain? = null

    fun update(configuration: EqualizerConfiguration) {
        processingChain = if (!configuration.enabled || configuration.gainsDb.values.all { abs(it) < 0.001f }) {
            null
        } else {
            ProcessingChain(
                channelCount = channelCount,
                filters = EqualizerBand.entries.map { band ->
                    BiquadCoefficients.peaking(
                        sampleRate = sampleRate.toDouble(),
                        centerFrequency = band.frequencyHz,
                        gainDb = configuration.gainFor(band).toDouble(),
                    )
                },
            )
        }
    }

    fun process(pcm: ByteArray): ByteArray {
        val chain = processingChain ?: return pcm
        var byteIndex = 0
        var channel = 0
        while (byteIndex + 1 < pcm.size) {
            val raw = ((pcm[byteIndex].toInt() and 0xff) or (pcm[byteIndex + 1].toInt() shl 8)).toShort()
            val filtered = chain.process(channel, raw.toDouble())
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            pcm[byteIndex] = (filtered and 0xff).toByte()
            pcm[byteIndex + 1] = ((filtered ushr 8) and 0xff).toByte()
            byteIndex += 2
            channel = (channel + 1) % channelCount
        }
        return pcm
    }
}

private class ProcessingChain(channelCount: Int, filters: List<BiquadCoefficients>) {
    private val channels = Array(channelCount) { filters.map(::BiquadFilter) }

    fun process(channel: Int, input: Double): Double = channels[channel].fold(input) { sample, filter ->
        filter.process(sample)
    }
}

private data class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    companion object {
        fun peaking(sampleRate: Double, centerFrequency: Double, gainDb: Double): BiquadCoefficients {
            if (abs(gainDb) < 0.0001) return BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0)
            val amplitude = 10.0.pow(gainDb / 40.0)
            val omega = 2.0 * PI * centerFrequency.coerceAtMost(sampleRate * 0.49) / sampleRate
            val alpha = sin(omega) / (2.0 * 1.1)
            val a0 = 1.0 + alpha / amplitude
            return BiquadCoefficients(
                b0 = (1.0 + alpha * amplitude) / a0,
                b1 = (-2.0 * cos(omega)) / a0,
                b2 = (1.0 - alpha * amplitude) / a0,
                a1 = (-2.0 * cos(omega)) / a0,
                a2 = (1.0 - alpha / amplitude) / a0,
            )
        }
    }
}

private class BiquadFilter(private val coefficients: BiquadCoefficients) {
    private var x1 = 0.0
    private var x2 = 0.0
    private var y1 = 0.0
    private var y2 = 0.0

    fun process(input: Double): Double {
        val output = coefficients.b0 * input + coefficients.b1 * x1 + coefficients.b2 * x2 -
            coefficients.a1 * y1 - coefficients.a2 * y2
        x2 = x1
        x1 = input
        y2 = y1
        y1 = output
        return output
    }
}
