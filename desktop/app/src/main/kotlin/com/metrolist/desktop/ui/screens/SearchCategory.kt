package com.metrolist.desktop.ui.screens

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem

enum class SearchCategory(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    PLAYLISTS("Playlists");

    fun accepts(item: YTItem): Boolean = when (this) {
        ALL -> item is SongItem || item is AlbumItem || item is ArtistItem || item is PlaylistItem
        SONGS -> item is SongItem
        ALBUMS -> item is AlbumItem
        ARTISTS -> item is ArtistItem
        PLAYLISTS -> item is PlaylistItem
    }
}
