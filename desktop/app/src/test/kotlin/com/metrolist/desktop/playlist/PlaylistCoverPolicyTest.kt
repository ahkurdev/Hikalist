package com.metrolist.desktop.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCoverPolicyTest {
    @Test
    fun `automatic cover uses at most four distinct song images`() {
        val cover = PlaylistCoverPolicy.resolve(
            customCover = null,
            songThumbnails = listOf("one", "one", "two", "three", "four", "five"),
        )

        assertEquals(listOf("one", "two", "three", "four"), (cover as PlaylistCover.Mosaic).urls)
    }

    @Test
    fun `custom cover wins and empty playlist uses flower fallback`() {
        assertEquals(PlaylistCover.Custom("custom.webp"), PlaylistCoverPolicy.resolve("custom.webp", listOf("one")))
        assertTrue(PlaylistCoverPolicy.resolve(null, emptyList()) is PlaylistCover.Fallback)
    }
}
