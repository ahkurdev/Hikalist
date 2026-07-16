package com.metrolist.desktop.lyrics

import com.metrolist.lrclib.LrcLib
import com.metrolist.music.betterlyrics.BetterLyrics
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

data class LyricsRequest(
    val videoId: String,
    val title: String,
    val artist: String,
    val durationSeconds: Int,
    val album: String?,
)

fun interface LyricsSource {
    suspend fun fetch(request: LyricsRequest): String?
}

class LyricsResolver(
    private val sources: List<LyricsSource>,
    private val timeoutMillis: Long = DEFAULT_TIMEOUT_MILLIS,
    cacheCapacity: Int = DEFAULT_CACHE_CAPACITY,
    private val persistentCache: LyricsCacheStore? = null,
) {
    private val cache = object : LinkedHashMap<String, String>(cacheCapacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > cacheCapacity
    }

    suspend fun resolve(request: LyricsRequest): String? {
        synchronized(cache) { cache[request.videoId] }?.let { return it }
        persistentCache?.read(request.videoId)?.let { storedLyrics ->
            validLyrics(storedLyrics)?.let { validStoredLyrics ->
                synchronized(cache) { cache[request.videoId] = validStoredLyrics }
                return validStoredLyrics
            }
            persistentCache.remove(request.videoId)
        }
        if (sources.isEmpty()) return null

        val result = fetchFastest(request) ?: return null
        synchronized(cache) { cache[request.videoId] = result }
        persistentCache?.write(request.videoId, result)
        return result
    }

    private suspend fun fetchFastest(request: LyricsRequest): String? = supervisorScope {
        val responses = Channel<String?>(sources.size)
        val jobs = sources.map { source ->
            launch {
                val lyrics = runCatching { source.fetch(request) }
                    .getOrNull()
                    ?.let(::validLyrics)
                responses.trySend(lyrics)
            }
        }

        var winner: String? = null
        withTimeoutOrNull(timeoutMillis) {
            repeat(sources.size) {
                val candidate = responses.receive()
                if (candidate != null) {
                    winner = candidate
                    return@withTimeoutOrNull
                }
            }
        }
        jobs.forEach { it.cancel() }
        responses.close()
        winner
    }

    private fun validLyrics(lyrics: String): String? = lyrics.trim()
        .takeIf { it.isNotEmpty() && SyncedLyricsParser.parse(it).isNotEmpty() }

    companion object {
        private const val DEFAULT_TIMEOUT_MILLIS = 7_000L
        private const val DEFAULT_CACHE_CAPACITY = 150

        fun default(): LyricsResolver = LyricsResolver(
            sources = listOf(
                LyricsSource { request ->
                    LrcLib.getLyrics(
                        title = request.title,
                        artist = request.artist,
                        duration = request.durationSeconds,
                        album = request.album,
                    ).getOrNull()
                },
                LyricsSource { request ->
                    BetterLyrics.getLyrics(
                        title = request.title,
                        artist = request.artist,
                        duration = request.durationSeconds,
                        album = request.album,
                    ).getOrNull()
                },
            ),
            persistentCache = LyricsCacheStore(),
        )
    }
}
