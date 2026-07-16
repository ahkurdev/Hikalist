package com.metrolist.desktop.spotify

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

fun interface SpotifyMetadataResolver {
    suspend fun resolve(reference: SpotifyTrackReference): SpotifyTrackMetadata
}

fun interface YouTubeSongSearcher {
    suspend fun search(metadata: SpotifyTrackMetadata): List<SongItem>
}

data class SpotifyImportResult(
    val reference: SpotifyTrackReference,
    val metadata: SpotifyTrackMetadata? = null,
    val song: SongItem? = null,
    val error: String? = null,
)

class SpotifyPlaylistImportService(
    private val metadataResolver: SpotifyMetadataResolver = SpotifyPublicMetadataResolver(),
    private val songSearcher: YouTubeSongSearcher = YouTubeMusicSongSearcher,
    private val parallelism: Int = 4,
) {
    suspend fun import(
        references: List<SpotifyTrackReference>,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> },
    ): List<SpotifyImportResult> = coroutineScope {
        val semaphore = Semaphore(parallelism.coerceAtLeast(1))
        val completed = AtomicInteger(0)
        references.map { reference ->
            async {
                semaphore.withPermit {
                    val result = runCatching {
                        val metadata = metadataResolver.resolve(reference)
                        val song = SpotifySongMatcher.bestMatch(metadata, songSearcher.search(metadata))
                        SpotifyImportResult(
                            reference = reference,
                            metadata = metadata,
                            song = song,
                            error = if (song == null) "No confident YouTube Music match" else null,
                        )
                    }.getOrElse { error ->
                        SpotifyImportResult(
                            reference = reference,
                            error = error.message ?: "Could not import this Spotify track",
                        )
                    }
                    onProgress(completed.incrementAndGet(), references.size)
                    result
                }
            }
        }.awaitAll()
    }
}

class SpotifyPublicMetadataResolver(
    private val client: HttpClient = createSpotifyMetadataClient(),
) : SpotifyMetadataResolver {
    override suspend fun resolve(reference: SpotifyTrackReference): SpotifyTrackMetadata {
        val html: String = client.get(reference.publicUrl).body()
        return SpotifyMetadataParser.parse(reference, html)
    }
}

private fun createSpotifyMetadataClient() = HttpClient(CIO) {
    expectSuccess = true
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 2)
        exponentialDelay()
    }
    defaultRequest {
        header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Hikalist/1.0")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.8")
    }
}

private object YouTubeMusicSongSearcher : YouTubeSongSearcher {
    override suspend fun search(metadata: SpotifyTrackMetadata): List<SongItem> =
        YouTube.search(
            "${metadata.title} ${metadata.artist}",
            YouTube.SearchFilter.FILTER_SONG,
        ).getOrThrow().items.filterIsInstance<SongItem>()
}

object SpotifySongMatcher {
    private const val MINIMUM_SCORE = 75.0

    fun bestMatch(metadata: SpotifyTrackMetadata, candidates: List<SongItem>): SongItem? =
        candidates
            .map { candidate -> candidate to score(metadata, candidate) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= MINIMUM_SCORE }
            ?.first

    private fun score(metadata: SpotifyTrackMetadata, candidate: SongItem): Double {
        val expectedTitle = normalize(metadata.title)
        val actualTitle = normalize(candidate.title)
        val expectedArtist = normalize(metadata.artist)
        val actualArtists = candidate.artists.joinToString(" ") { normalize(it.name) }

        val titleScore = when {
            expectedTitle == actualTitle -> 70.0
            expectedTitle in actualTitle || actualTitle in expectedTitle -> 58.0
            else -> tokenSimilarity(expectedTitle, actualTitle) * 55.0
        }
        val artistScore = when {
            expectedArtist == actualArtists -> 30.0
            expectedArtist in actualArtists || actualArtists in expectedArtist -> 27.0
            else -> tokenSimilarity(expectedArtist, actualArtists) * 25.0
        }
        return titleScore + artistScore
    }

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .replace(Regex("\\p{M}+"), "")
        .lowercase()
        .replace("&", " and ")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()

    private fun tokenSimilarity(first: String, second: String): Double {
        val firstTokens = first.split(' ').filter(String::isNotBlank).toSet()
        val secondTokens = second.split(' ').filter(String::isNotBlank).toSet()
        if (firstTokens.isEmpty() || secondTokens.isEmpty()) return 0.0
        return firstTokens.intersect(secondTokens).size.toDouble() / firstTokens.union(secondTokens).size
    }
}
