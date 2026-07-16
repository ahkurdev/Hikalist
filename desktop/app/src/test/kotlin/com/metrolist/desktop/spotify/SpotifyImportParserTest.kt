package com.metrolist.desktop.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyImportParserTest {
    @Test
    fun `parses copied track links and preserves their order`() {
        val input = """
            https://open.spotify.com/track/5WOSNVChcadlsCRiqXE45K
            https://open.spotify.com/track/6kjnGjYMk9YAtH1BMD311b?si=share-token
            https://open.spotify.com/track/0u4rkpmNtgcFxYHepnVF4v
        """.trimIndent()

        val result = SpotifyImportParser.parse(input)

        assertEquals(
            listOf(
                "5WOSNVChcadlsCRiqXE45K",
                "6kjnGjYMk9YAtH1BMD311b",
                "0u4rkpmNtgcFxYHepnVF4v",
            ),
            result.tracks.map(SpotifyTrackReference::id),
        )
        assertEquals(0, result.ignoredEntries)
        assertEquals(0, result.duplicateEntries)
    }

    @Test
    fun `accepts spotify uris and ignores duplicates and unsupported entries`() {
        val input = """
            spotify:track:5WOSNVChcadlsCRiqXE45K
            https://open.spotify.com/track/5WOSNVChcadlsCRiqXE45K
            https://open.spotify.com/album/4m2880jivSbbyEGAKfITCa
            not-a-link
        """.trimIndent()

        val result = SpotifyImportParser.parse(input)

        assertEquals(listOf("5WOSNVChcadlsCRiqXE45K"), result.tracks.map(SpotifyTrackReference::id))
        assertEquals(2, result.ignoredEntries)
        assertEquals(1, result.duplicateEntries)
    }

    @Test
    fun `extracts every track link when pasted on one line`() {
        val result = SpotifyImportParser.parse(
            "https://open.spotify.com/track/5WOSNVChcadlsCRiqXE45K " +
                "https://open.spotify.com/track/6kjnGjYMk9YAtH1BMD311b",
        )

        assertEquals(2, result.tracks.size)
    }

    @Test
    fun `extracts title artist album and year from public spotify metadata`() {
        val html = """
            <html><head>
              <meta property="og:title" content="everything u are"/>
              <meta property="og:description" content="Hindia · Doves, &amp;#x27;25 on Blank Canvas · Song · 2025"/>
              <meta property="og:image" content="https://i.scdn.co/image/cover"/>
            </head></html>
        """.trimIndent()

        val metadata = SpotifyMetadataParser.parse(
            SpotifyTrackReference("5WOSNVChcadlsCRiqXE45K"),
            html,
        )

        assertEquals("everything u are", metadata.title)
        assertEquals("Hindia", metadata.artist)
        assertEquals("Doves, '25 on Blank Canvas", metadata.album)
        assertEquals(2025, metadata.year)
        assertEquals("https://i.scdn.co/image/cover", metadata.coverUrl)
    }

    @Test
    fun `rejects a spotify response without usable metadata`() {
        val error = runCatching {
            SpotifyMetadataParser.parse(SpotifyTrackReference("5WOSNVChcadlsCRiqXE45K"), "<html></html>")
        }.exceptionOrNull()

        assertTrue(error is SpotifyMetadataException)
    }
}
