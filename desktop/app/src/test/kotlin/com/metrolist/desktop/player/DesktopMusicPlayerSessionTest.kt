package com.metrolist.desktop.player

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DesktopMusicPlayerSessionTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `restored queue remains stopped until user resumes it`() {
        val store = QueueSessionStore(File(temporaryFolder.root, "queue.json"))
        store.save(
            QueueSessionSnapshot(
                items = listOf(QueueSessionItem("video", null, "Ada", "Lyodra", "Album", "cover", 211, null, null)),
                currentIndex = 0,
                positionSeconds = 34.25,
                shuffled = false,
                repeatMode = RepeatMode.ALL,
            ),
        )

        val player = DesktopMusicPlayer(ffmpegPathProvider = { null }, queueSessionStore = store)

        assertEquals("video", player.currentSong.value?.item?.videoId)
        assertEquals(34.25, player.position.value, 0.001)
        assertEquals(DesktopMusicPlayer.State.IDLE, player.state.value)
        assertFalse(player.shuffle.value)
        player.dispose()
    }
}
