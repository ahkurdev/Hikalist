package com.metrolist.desktop.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncedLyricsParserTest {
    @Test
    fun `parses synced and plain lyric lines safely`() {
        val parsed = SyncedLyricsParser.parse("[00:03.20]First line\n[01:02.005]Second line")

        assertEquals(listOf(3_200L, 62_005L), parsed.map { it.timeMs })
        assertEquals(listOf("First line", "Second line"), parsed.map { it.text })
        assertEquals(1, SyncedLyricsParser.activeIndex(parsed, positionMs = 63_000L))
    }

    @Test
    fun `plain lyrics remain readable without fake timing`() {
        val parsed = SyncedLyricsParser.parse("First line\n\nSecond line")

        assertEquals(listOf(null, null), parsed.map { it.timeMs })
        assertEquals(listOf("First line", "Second line"), parsed.map { it.text })
        assertEquals(-1, SyncedLyricsParser.activeIndex(parsed, positionMs = 20_000L))
    }

    @Test
    fun `waiting indicator only appears before first synced line`() {
        val synced = SyncedLyricsParser.parse("[00:03.20]First line\n[00:08.00]Second line")
        val plain = SyncedLyricsParser.parse("First line\nSecond line")

        assertTrue(SyncedLyricsParser.isWaitingForFirstTimedLine(synced, positionMs = 3_199L))
        assertFalse(SyncedLyricsParser.isWaitingForFirstTimedLine(synced, positionMs = 3_200L))
        assertFalse(SyncedLyricsParser.isWaitingForFirstTimedLine(plain, positionMs = 0L))
    }

    @Test
    fun `better lyrics word timing metadata is hidden from displayed lines`() {
        val parsed = SyncedLyricsParser.parse(
            """
            [00:13.43]{agent:v1}Ku tahu kisah kita sedang diuji waktu
            <Ku:13.433:13.929|tahu:13.929:15.947|kisah:16.575:17.327|kita:17.327:18.476|sedang:18.476:19.502|diuji:19.684:21.204|waktu:21.204:23.501>
            [00:25.58]{agent:v2}Seperti tak pernah terbayang
            <Seperti:25.584:28.381|tak:28.706:29.106|pernah:29.106:30.239|terbayang:30.239:31.465>
            """.trimIndent(),
        )

        assertEquals(listOf(13_430L, 25_580L), parsed.map(LyricLine::timeMs))
        assertEquals(
            listOf("Ku tahu kisah kita sedang diuji waktu", "Seperti tak pernah terbayang"),
            parsed.map(LyricLine::text),
        )
    }
}
