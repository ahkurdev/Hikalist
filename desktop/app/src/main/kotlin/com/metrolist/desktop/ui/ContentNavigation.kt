package com.metrolist.desktop.ui

import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.YTItem

sealed interface ContentDestination {
    val id: String

    data class Album(override val id: String) : ContentDestination
    data class Artist(override val id: String) : ContentDestination
    data class Playlist(override val id: String) : ContentDestination
}

data class ContentHistory(
    private val entries: List<ContentDestination> = emptyList(),
) {
    val current: ContentDestination?
        get() = entries.lastOrNull()

    fun open(destination: ContentDestination) = copy(entries = entries + destination)

    fun back() = copy(entries = entries.dropLast(1))

    fun clear() = ContentHistory()
}

fun contentDestinationFor(item: YTItem): ContentDestination? = when (item) {
    is AlbumItem -> ContentDestination.Album(item.browseId)
    is ArtistItem -> ContentDestination.Artist(item.id)
    is PlaylistItem -> ContentDestination.Playlist(item.id.removePrefix("VL"))
    else -> null
}
