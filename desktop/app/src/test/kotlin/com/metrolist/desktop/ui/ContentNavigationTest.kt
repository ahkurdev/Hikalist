package com.metrolist.desktop.ui

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentNavigationTest {
    @Test
    fun `albums artists and playlists map to remote destinations`() {
        val album = AlbumItem("album-id", "playlist-id", title = "Album", artists = null, thumbnail = "album.jpg")
        val artist = ArtistItem("artist-id", "Artist", "artist.jpg", shuffleEndpoint = null, radioEndpoint = null)
        val playlist = PlaylistItem("playlist-id", "Playlist", Artist("Owner", null), null, "playlist.jpg", null, null, null)

        assertEquals(ContentDestination.Album("album-id"), contentDestinationFor(album))
        assertEquals(ContentDestination.Artist("artist-id"), contentDestinationFor(artist))
        assertEquals(ContentDestination.Playlist("playlist-id"), contentDestinationFor(playlist))
    }

    @Test
    fun `songs stay playback actions rather than navigation destinations`() {
        val song = SongItem("song-id", "Song", emptyList(), thumbnail = "song.jpg")

        assertNull(contentDestinationFor(song))
    }

    @Test
    fun `history returns through nested content and then becomes empty`() {
        val album = ContentDestination.Album("album-id")
        val artist = ContentDestination.Artist("artist-id")

        val nested = ContentHistory().open(artist).open(album)
        val returned = nested.back()

        assertEquals(album, nested.current)
        assertEquals(artist, returned.current)
        assertNull(returned.back().current)
    }
}
