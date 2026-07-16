package com.metrolist.desktop.ui

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class RemoteContentProbeTest {
    @Test
    fun `anonymous album artist and playlist pages load playable content`() = runBlocking {
        assumeTrue(
            "Set METROLIST_NETWORK_TESTS=1 to run the real content navigation probe",
            System.getenv("METROLIST_NETWORK_TESTS") == "1",
        )

        val album = YouTube.search("Adele", YouTube.SearchFilter.FILTER_ALBUM).getOrThrow()
            .items.filterIsInstance<AlbumItem>().first()
        val albumPage = YouTube.album(album.browseId).getOrThrow()
        assertTrue(albumPage.songs.isNotEmpty())

        val artist = YouTube.search("Adele", YouTube.SearchFilter.FILTER_ARTIST).getOrThrow()
            .items.filterIsInstance<ArtistItem>().first()
        val artistPage = YouTube.artist(artist.id).getOrThrow()
        assertTrue(artistPage.sections.isNotEmpty())

        val playlist = YouTube.search("Adele", YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrThrow()
            .items.filterIsInstance<PlaylistItem>().first()
        val playlistPage = YouTube.playlist(playlist.id).getOrThrow()
        assertNotNull(playlistPage.playlist.title)
        assertTrue(playlistPage.songs.isNotEmpty())
    }
}
