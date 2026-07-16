package com.metrolist.desktop.playlist

sealed interface PlaylistCover {
    data class Custom(val url: String) : PlaylistCover
    data class Mosaic(val urls: List<String>) : PlaylistCover
    data object Fallback : PlaylistCover
}

object PlaylistCoverPolicy {
    fun resolve(customCover: String?, songThumbnails: List<String?>): PlaylistCover {
        if (!customCover.isNullOrBlank()) return PlaylistCover.Custom(customCover)
        val images = songThumbnails.filterNotNull().filter(String::isNotBlank).distinct().take(4)
        return if (images.isEmpty()) PlaylistCover.Fallback else PlaylistCover.Mosaic(images)
    }
}
