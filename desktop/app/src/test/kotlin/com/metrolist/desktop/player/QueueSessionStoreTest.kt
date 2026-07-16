package com.metrolist.desktop.player

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class QueueSessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `queue session survives store restart with playback position`() {
        val file = File(temporaryFolder.root, "queue-session.json")
        val snapshot = QueueSessionSnapshot(
            items = listOf(
                QueueSessionItem("one", "playlist", "Song One", "Artist", "Album", "cover", 210, "Allan4u", 123L),
                QueueSessionItem("two", null, "Song Two", "Artist Two", null, null, 180, null, null),
            ),
            currentIndex = 1,
            positionSeconds = 42.5,
            shuffled = true,
            repeatMode = RepeatMode.ONE,
        )

        QueueSessionStore(file).save(snapshot)

        assertEquals(snapshot, QueueSessionStore(file).load())
    }

    @Test
    fun `invalid queue session is ignored safely`() {
        val file = File(temporaryFolder.root, "queue-session.json").apply { writeText("not-json") }

        assertNull(QueueSessionStore(file).load())
    }

    @Test
    fun `empty queue clears persisted session`() {
        val file = File(temporaryFolder.root, "queue-session.json")
        val store = QueueSessionStore(file)
        store.save(QueueSessionSnapshot(emptyList(), -1, 0.0, false, RepeatMode.ALL))

        assertNull(store.load())
        assertEquals(false, file.exists())
    }
}
