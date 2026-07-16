package com.metrolist.desktop.lyrics

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricsResolverTest {
    private val request = LyricsRequest(
        videoId = "video-id",
        title = "Test Song",
        artist = "Test Artist",
        durationSeconds = 180,
        album = "Test Album",
    )

    @Test
    fun `returns the fastest valid provider result`() = runBlocking {
        val resolver = LyricsResolver(
            sources = listOf(
                LyricsSource {
                    delay(150)
                    "[00:01.00]slow"
                },
                LyricsSource {
                    delay(15)
                    "[00:01.00]fast"
                },
            ),
            timeoutMillis = 500,
        )

        assertEquals("[00:01.00]fast", resolver.resolve(request))
    }

    @Test
    fun `failed provider does not block another provider`() = runBlocking {
        val resolver = LyricsResolver(
            sources = listOf(
                LyricsSource { error("provider failed") },
                LyricsSource { "[00:01.00]available" },
            ),
            timeoutMillis = 500,
        )

        assertEquals("[00:01.00]available", resolver.resolve(request))
    }

    @Test
    fun `successful result is served from memory cache`() = runBlocking {
        var calls = 0
        val resolver = LyricsResolver(
            sources = listOf(
                LyricsSource {
                    calls += 1
                    "[00:01.00]cached"
                },
            ),
            timeoutMillis = 500,
        )

        assertEquals("[00:01.00]cached", resolver.resolve(request))
        assertEquals("[00:01.00]cached", resolver.resolve(request))
        assertEquals(1, calls)
    }

    @Test
    fun `successful result survives resolver restart`() = runBlocking {
        val cacheDirectory = Files.createTempDirectory("hikalist-lyrics-cache-test").toFile()
        val firstResolver = LyricsResolver(
            sources = listOf(LyricsSource { "[00:01.00]persisted" }),
            timeoutMillis = 500,
            persistentCache = LyricsCacheStore(cacheDirectory),
        )

        assertEquals("[00:01.00]persisted", firstResolver.resolve(request))

        var providerCalls = 0
        val restartedResolver = LyricsResolver(
            sources = listOf(
                LyricsSource {
                    providerCalls += 1
                    null
                },
            ),
            timeoutMillis = 500,
            persistentCache = LyricsCacheStore(cacheDirectory),
        )

        assertEquals("[00:01.00]persisted", restartedResolver.resolve(request))
        assertEquals(0, providerCalls)
    }

    @Test
    fun `returns null when all providers exceed timeout`() = runBlocking {
        val resolver = LyricsResolver(
            sources = listOf(
                LyricsSource {
                    delay(250)
                    "[00:01.00]late"
                },
            ),
            timeoutMillis = 25,
        )

        assertNull(resolver.resolve(request))
    }
}
