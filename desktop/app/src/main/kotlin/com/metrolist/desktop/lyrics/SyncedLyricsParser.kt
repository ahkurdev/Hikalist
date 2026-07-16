package com.metrolist.desktop.lyrics

data class LyricLine(val timeMs: Long?, val text: String)

object SyncedLyricsParser {
    private val timestamp = Regex("^\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?](.*)$")
    private val displayMarkers = Regex("^(?:(?:\\{agent:[^}]+})|(?:\\{bg}))+", RegexOption.IGNORE_CASE)
    private val wordTiming = Regex(":[0-9]+(?:\\.[0-9]+)?:[0-9]+(?:\\.[0-9]+)?(?:\\||>)")

    fun parse(lyrics: String): List<LyricLine> = lyrics.lineSequence().mapNotNull { rawLine ->
        val line = rawLine.trim()
        if (line.isBlank()) return@mapNotNull null
        if (line.startsWith('<') && line.endsWith('>') && wordTiming.containsMatchIn(line)) {
            return@mapNotNull null
        }
        val match = timestamp.matchEntire(line)
        if (match == null) return@mapNotNull LyricLine(null, cleanDisplayText(line))
        val (minutes, seconds, fraction, text) = match.destructured
        val fractionMs = when (fraction.length) {
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            3 -> fraction.toLong()
            else -> 0
        }
        LyricLine(
            minutes.toLong() * 60_000 + seconds.toLong() * 1_000 + fractionMs,
            cleanDisplayText(text),
        )
    }.filter { it.text.isNotBlank() }.toList()

    private fun cleanDisplayText(text: String): String = text.trim()
        .replaceFirst(displayMarkers, "")
        .trim()

    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int =
        lines.indexOfLast { line -> line.timeMs?.let { it <= positionMs } == true }

    fun isWaitingForFirstTimedLine(lines: List<LyricLine>, positionMs: Long): Boolean =
        lines.any { it.timeMs != null } && activeIndex(lines, positionMs) == -1
}
