package com.metrolist.desktop.db.repository

import java.nio.file.Files
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.Test

class DataRepositoryPlaylistImportTest {
    private val repository = DataRepository()

    @Test
    fun `adds imported songs in spotify order and ignores duplicate ids`() {
        val playlist = repository.createPlaylist("Imported")
        val songs = listOf(
            SongRow("import-one", "One", artists = listOf("Artist")),
            SongRow("import-two", "Two", artists = listOf("Artist")),
            SongRow("import-three", "Three", artists = listOf("Artist")),
            SongRow("import-two", "Two", artists = listOf("Artist")),
        )

        repository.addSongsToPlaylist(playlist.id, songs)

        assertEquals(
            listOf("import-one", "import-two", "import-three"),
            repository.getPlaylistSongs(playlist.id).map(SongRow::id),
        )
    }

    companion object {
        private lateinit var originalHome: String

        @JvmStatic
        @BeforeClass
        fun useTemporaryDatabase() {
            originalHome = System.getProperty("user.home")
            System.setProperty("user.home", Files.createTempDirectory("hikalist-import-test").toString())
        }

        @JvmStatic
        @AfterClass
        fun restoreHomeDirectory() {
            System.setProperty("user.home", originalHome)
        }
    }
}
