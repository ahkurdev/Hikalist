package com.metrolist.desktop.playlist

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistMetadataStoreTest {
    @Test
    fun `description persists after store is reopened`() {
        val metadataFile = Files.createTempDirectory("hikalist-metadata-test")
            .resolve("playlist-metadata.properties")
            .toFile()

        PlaylistMetadataStore(metadataFile).setDescription("playlist-1", "  Late-night favorites  ")

        assertEquals("Late-night favorites", PlaylistMetadataStore(metadataFile).getDescription("playlist-1"))
    }

    @Test
    fun `blank description and delete remove playlist metadata`() {
        val metadataFile = Files.createTempDirectory("hikalist-metadata-delete-test")
            .resolve("playlist-metadata.properties")
            .toFile()
        val store = PlaylistMetadataStore(metadataFile)

        store.setDescription("playlist-1", "First")
        store.setDescription("playlist-1", "   ")
        store.setDescription("playlist-2", "Second")
        store.delete("playlist-2")

        assertNull(store.getDescription("playlist-1"))
        assertNull(store.getDescription("playlist-2"))
    }
}
