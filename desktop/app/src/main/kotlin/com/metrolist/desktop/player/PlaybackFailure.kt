package com.metrolist.desktop.player

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.sound.sampled.LineUnavailableException

enum class PlaybackFailureKind {
    NETWORK,
    STREAM_UNAVAILABLE,
    DECODER,
    AUDIO_DEVICE,
    UNKNOWN,
}

data class PlaybackFailure(
    val kind: PlaybackFailureKind,
    val title: String,
    val message: String,
    val retryable: Boolean,
)

sealed interface PlaybackNotice {
    data class Retrying(
        val failure: PlaybackFailure,
        val attempt: Int,
        val maxAttempts: Int,
    ) : PlaybackNotice

    data class Failed(val failure: PlaybackFailure) : PlaybackNotice
}

object PlaybackFailureClassifier {
    fun classify(error: Throwable): PlaybackFailure {
        val causes = generateSequence(error) { it.cause }.toList()
        val details = causes.joinToString(" ") { it.message.orEmpty() }.lowercase()

        return when {
            causes.any { it is LineUnavailableException } -> PlaybackFailure(
                kind = PlaybackFailureKind.AUDIO_DEVICE,
                title = "Audio device unavailable",
                message = "Check your Windows output device and try again.",
                retryable = false,
            )
            DECODER_MARKERS.any(details::contains) -> PlaybackFailure(
                kind = PlaybackFailureKind.DECODER,
                title = "Audio could not be decoded",
                message = "Hikalist could not process this audio stream.",
                retryable = true,
            )
            STREAM_MARKERS.any(details::contains) -> PlaybackFailure(
                kind = PlaybackFailureKind.STREAM_UNAVAILABLE,
                title = "Audio stream unavailable",
                message = "This song's stream could not be opened.",
                retryable = true,
            )
            causes.any { it is SocketException || it is SocketTimeoutException || it is UnknownHostException } ||
                NETWORK_MARKERS.any(details::contains) -> PlaybackFailure(
                kind = PlaybackFailureKind.NETWORK,
                title = "Connection interrupted",
                message = "Hikalist could not reach the audio stream.",
                retryable = true,
            )
            else -> PlaybackFailure(
                kind = PlaybackFailureKind.UNKNOWN,
                title = "Playback failed",
                message = "Something stopped this song from playing.",
                retryable = false,
            )
        }
    }

    private val NETWORK_MARKERS = listOf("timeout", "timed out", "connection reset", "connection refused", "network")
    private val STREAM_MARKERS = listOf("stream is unavailable", "video is unavailable", "playability", "stream url")
    private val DECODER_MARKERS = listOf("ffmpeg", "decode", "decoded", "pcm", "audio line")
}

sealed interface RetryDecision {
    data class Retry(val attempt: Int, val delayMillis: Long) : RetryDecision
    data object Stop : RetryDecision
}

class PlaybackRetryPolicy(
    private val retryDelaysMillis: List<Long> = listOf(1_000L, 2_500L),
) {
    val maxRetries: Int
        get() = retryDelaysMillis.size

    fun decide(failure: PlaybackFailure, failedAttempt: Int): RetryDecision {
        if (!failure.retryable) return RetryDecision.Stop
        val delay = retryDelaysMillis.getOrNull(failedAttempt) ?: return RetryDecision.Stop
        return RetryDecision.Retry(attempt = failedAttempt + 1, delayMillis = delay)
    }
}
