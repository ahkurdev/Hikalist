package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSeekPolicyTest {
    @Test
    fun `ordinary seek target is preserved`() {
        assertEquals(180.0, PlaybackSeekPolicy.normalize(180.0, 240.0), 0.001)
    }

    @Test
    fun `seeking to the endpoint plays the final two seconds`() {
        assertEquals(238.0, PlaybackSeekPolicy.normalize(240.0, 240.0), 0.001)
    }

    @Test
    fun `early ffmpeg eof is not treated as natural completion`() {
        assertFalse(PlaybackSeekPolicy.reachedNaturalEnd(positionSeconds = 181.0, durationSeconds = 240.0))
    }

    @Test
    fun `ffmpeg eof close to duration is natural completion`() {
        assertTrue(PlaybackSeekPolicy.reachedNaturalEnd(positionSeconds = 239.4, durationSeconds = 240.0))
    }

    @Test
    fun `resolved stream cache reuses only the current song stream`() {
        val cache = PlaybackStreamCache<Any>()
        val first = Any()
        cache.put("song-1", first)

        assertSame(first, cache.get("song-1"))
        assertNull(cache.get("song-2"))

        cache.clear()
        assertNull(cache.get("song-1"))
    }
}
