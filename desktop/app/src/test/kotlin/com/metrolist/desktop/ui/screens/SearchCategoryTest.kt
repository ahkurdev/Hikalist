package com.metrolist.desktop.ui.screens

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchCategoryTest {
    private val song = SongItem("song", "Song", emptyList(), thumbnail = "song.jpg")
    private val album = AlbumItem("album", "list", title = "Album", artists = null, thumbnail = "album.jpg")
    private val artist = ArtistItem("artist", "Artist", null, shuffleEndpoint = null, radioEndpoint = null)
    private val playlist = PlaylistItem("playlist", "Playlist", Artist("Owner", null), null, null, null, null, null)

    @Test
    fun `all accepts every supported music result`() {
        assertTrue(listOf(song, album, artist, playlist).all(SearchCategory.ALL::accepts))
    }

    @Test
    fun `specific categories only accept their own item type`() {
        assertTrue(SearchCategory.SONGS.accepts(song))
        assertFalse(SearchCategory.SONGS.accepts(album))
        assertTrue(SearchCategory.ALBUMS.accepts(album))
        assertTrue(SearchCategory.ARTISTS.accepts(artist))
        assertTrue(SearchCategory.PLAYLISTS.accepts(playlist))
    }
}
