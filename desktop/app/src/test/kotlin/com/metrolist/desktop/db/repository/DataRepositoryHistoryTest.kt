package com.metrolist.desktop.db.repository

import java.nio.file.Files
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class DataRepositoryHistoryTest {
    private val repository = DataRepository()

    @Test
    fun `repeated search becomes the most recent query`() {
        repository.clearSearchHistory()

        repository.addSearchQuery("Adele")
        repository.addSearchQuery("NOAH")
        repository.addSearchQuery("adele")

        assertEquals(listOf("adele", "NOAH"), repository.getSearchHistory().take(2))
    }

    @Test
    fun `recent played songs are unique and newest first`() {
        val first = SongRow(id = "history-first", title = "First", artists = listOf("Artist A"))
        val second = SongRow(id = "history-second", title = "Second", artists = listOf("Artist B"))
        repository.saveSong(first)
        repository.saveSong(second)

        repository.recordPlay(first.id, 0L)
        repository.recordPlay(second.id, 0L)
        repository.recordPlay(first.id, 0L)

        assertEquals(
            listOf(first.id, second.id),
            repository.getRecentPlayedSongs(limit = 2).map(SongRow::id),
        )
    }

    companion object {
        private lateinit var originalHome: String

        @JvmStatic
        @BeforeClass
        fun useTemporaryDatabase() {
            originalHome = System.getProperty("user.home")
            System.setProperty("user.home", Files.createTempDirectory("hikalist-history-test").toString())
        }

        @JvmStatic
        @AfterClass
        fun restoreHomeDirectory() {
            System.setProperty("user.home", originalHome)
        }
    }
}
