package com.metrolist.desktop.spotify

import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyImportServiceTest {
    @Test
    fun `matcher prefers exact title and artist over a similarly named result`() {
        val metadata = metadata("everything u are", "Hindia")
        val candidates = listOf(
            song("cover", "Everything You Are (Cover)", "Other Artist"),
            song("exact", "everything u are", "Hindia"),
        )

        assertEquals("exact", SpotifySongMatcher.bestMatch(metadata, candidates)?.id)
    }

    @Test
    fun `matcher rejects unrelated search results`() {
        val result = SpotifySongMatcher.bestMatch(
            metadata("everything u are", "Hindia"),
            listOf(song("wrong", "Bohemian Rhapsody", "Queen")),
        )

        assertNull(result)
    }

    @Test
    fun `import keeps spotify order and records individual failures`() = runBlocking {
        val first = SpotifyTrackReference("5WOSNVChcadlsCRiqXE45K")
        val second = SpotifyTrackReference("6kjnGjYMk9YAtH1BMD311b")
        val resolver = SpotifyMetadataResolver { reference ->
            if (reference == second) error("Spotify unavailable")
            metadata("everything u are", "Hindia", reference)
        }
        val searcher = YouTubeSongSearcher { listOf(song("youtube-1", "everything u are", "Hindia")) }

        val results = SpotifyPlaylistImportService(resolver, searcher).import(listOf(first, second))

        assertEquals(listOf(first, second), results.map(SpotifyImportResult::reference))
        assertEquals("youtube-1", results[0].song?.id)
        assertEquals("Spotify unavailable", results[1].error)
    }

    private fun metadata(
        title: String,
        artist: String,
        reference: SpotifyTrackReference = SpotifyTrackReference("5WOSNVChcadlsCRiqXE45K"),
    ) = SpotifyTrackMetadata(reference, title, artist, null, null, null)

    private fun song(id: String, title: String, artist: String) = SongItem(
        id = id,
        title = title,
        artists = listOf(Artist(artist, null)),
        thumbnail = "",
    )
}
