package com.metrolist.desktop.player

import java.io.IOException
import javax.sound.sampled.LineUnavailableException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFailurePolicyTest {
    @Test
    fun `network exceptions are retryable and understandable`() {
        val failure = PlaybackFailureClassifier.classify(IOException("Connection reset"))

        assertEquals(PlaybackFailureKind.NETWORK, failure.kind)
        assertEquals("Connection interrupted", failure.title)
        assertTrue(failure.retryable)
    }

    @Test
    fun `unavailable stream is classified separately`() {
        val failure = PlaybackFailureClassifier.classify(
            IllegalStateException("Video stream is unavailable"),
        )

        assertEquals(PlaybackFailureKind.STREAM_UNAVAILABLE, failure.kind)
        assertTrue(failure.retryable)
    }

    @Test
    fun `missing audio device stops automatic retry`() {
        val failure = PlaybackFailureClassifier.classify(LineUnavailableException("No mixer"))

        assertEquals(PlaybackFailureKind.AUDIO_DEVICE, failure.kind)
        assertFalse(failure.retryable)
    }

    @Test
    fun `ffmpeg process failure is not misclassified as network`() {
        val failure = PlaybackFailureClassifier.classify(
            IOException("Cannot run program hikalist_ffmpeg.exe: CreateProcess error=5"),
        )

        assertEquals(PlaybackFailureKind.DECODER, failure.kind)
    }

    @Test
    fun `retry policy retries twice then stops`() {
        val policy = PlaybackRetryPolicy(retryDelaysMillis = listOf(1_000L, 2_500L))
        val failure = PlaybackFailure(
            kind = PlaybackFailureKind.NETWORK,
            title = "Connection interrupted",
            message = "Check your connection.",
            retryable = true,
        )

        assertEquals(RetryDecision.Retry(attempt = 1, delayMillis = 1_000L), policy.decide(failure, failedAttempt = 0))
        assertEquals(RetryDecision.Retry(attempt = 2, delayMillis = 2_500L), policy.decide(failure, failedAttempt = 1))
        assertEquals(RetryDecision.Stop, policy.decide(failure, failedAttempt = 2))
    }

    @Test
    fun `non retryable failure stops immediately`() {
        val policy = PlaybackRetryPolicy()
        val failure = PlaybackFailure(
            kind = PlaybackFailureKind.UNKNOWN,
            title = "Playback failed",
            message = "Try again.",
            retryable = false,
        )

        assertEquals(RetryDecision.Stop, policy.decide(failure, failedAttempt = 0))
    }
}
