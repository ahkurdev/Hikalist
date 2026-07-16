package com.metrolist.desktop.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackQueueTest {
    private val songs = (1..4).map { index ->
        DesktopMusicPlayer.QueueItem(videoId = "song-$index", title = "Song $index")
    }

    @Test
    fun `selecting a source keeps its order and starts at selected song`() {
        val queue = PlaybackQueue()
        queue.repeatMode = RepeatMode.OFF

        queue.setSource(songs, selectedIndex = 2)

        assertEquals("song-3", queue.current?.videoId)
        assertEquals(listOf("song-1", "song-2", "song-3", "song-4"), queue.items.map { it.videoId })
        assertEquals("song-4", queue.moveNext()?.videoId)
        assertNull(queue.moveNext())
    }

    @Test
    fun `queue repeats all by default and wraps next to first song`() {
        val queue = PlaybackQueue()
        queue.setSource(songs.take(2), selectedIndex = 1)

        assertEquals(RepeatMode.ALL, queue.repeatMode)
        assertEquals("song-1", queue.moveNext()?.videoId)
    }

    @Test
    fun `repeat all wraps while repeat one keeps current song`() {
        val queue = PlaybackQueue()
        queue.setSource(songs.take(2), selectedIndex = 1)

        queue.repeatMode = RepeatMode.ALL
        assertEquals("song-1", queue.moveNext()?.videoId)

        queue.repeatMode = RepeatMode.ONE
        assertEquals("song-1", queue.moveNext(automatic = true)?.videoId)
    }

    @Test
    fun `shuffle preserves current and restores original source order`() {
        val queue = PlaybackQueue(randomSeed = 7)
        queue.setSource(songs, selectedIndex = 1)

        queue.setShuffle(true)
        assertTrue(queue.shuffled)
        assertEquals("song-2", queue.current?.videoId)
        assertEquals("song-2", queue.items.first().videoId)

        queue.setShuffle(false)
        assertFalse(queue.shuffled)
        assertEquals(listOf("song-1", "song-2", "song-3", "song-4"), queue.items.map { it.videoId })
        assertEquals("song-2", queue.current?.videoId)
    }

    @Test
    fun `previous restarts current after five seconds otherwise moves back`() {
        val queue = PlaybackQueue()
        queue.setSource(songs, selectedIndex = 2)

        assertEquals(PreviousAction.RESTART_CURRENT, queue.previousAction(positionSeconds = 6.0))
        assertEquals(PreviousAction.MOVE_PREVIOUS, queue.previousAction(positionSeconds = 2.0))
        assertEquals("song-2", queue.movePrevious()?.videoId)
    }

    @Test
    fun `moving queue rows preserves the active song`() {
        val queue = PlaybackQueue()
        queue.setSource(songs, selectedIndex = 1)

        queue.move(fromIndex = 3, toIndex = 0)

        assertEquals(listOf("song-4", "song-1", "song-2", "song-3"), queue.items.map { it.videoId })
        assertEquals("song-2", queue.current?.videoId)
    }

    @Test
    fun `play next moves selected row directly after active song`() {
        val queue = PlaybackQueue()
        queue.setSource(songs, selectedIndex = 1)

        queue.playNext(index = 3)

        assertEquals(listOf("song-1", "song-2", "song-4", "song-3"), queue.items.map { it.videoId })
        assertEquals("song-2", queue.current?.videoId)
        assertEquals("song-4", queue.moveNext()?.videoId)
    }
}
