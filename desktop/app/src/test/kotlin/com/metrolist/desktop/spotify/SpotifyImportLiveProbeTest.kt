package com.metrolist.desktop.spotify

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test

class SpotifyImportLiveProbeTest {
    @Test
    fun `public Spotify track resolves to a playable YouTube Music match`() = runBlocking {
        assumeTrue(System.getenv("HIKALIST_SPOTIFY_PROBE") == "1")
        val reference = SpotifyTrackReference("5WOSNVChcadlsCRiqXE45K")

        val result = SpotifyPlaylistImportService().import(listOf(reference)).single()

        assertEquals(reference, result.reference)
        assertNotNull("Spotify metadata failed: ${result.error}", result.metadata)
        assertNotNull("YouTube Music match failed: ${result.error}", result.song)
    }
}
