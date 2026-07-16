package com.metrolist.desktop.playlist

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

class PlaylistMetadataStore(
    private val metadataFile: File = File(System.getProperty("user.home"), ".metrolist/playlist-metadata.properties"),
) {
    @Synchronized
    fun getDescription(playlistId: String): String? =
        load().getProperty(descriptionKey(playlistId))?.trim()?.takeIf(String::isNotEmpty)

    @Synchronized
    fun setDescription(playlistId: String, description: String) {
        val properties = load()
        val value = description.trim()
        if (value.isEmpty()) properties.remove(descriptionKey(playlistId))
        else properties.setProperty(descriptionKey(playlistId), value)
        save(properties)
    }

    @Synchronized
    fun delete(playlistId: String) {
        val properties = load()
        properties.remove(descriptionKey(playlistId))
        save(properties)
    }

    private fun load() = Properties().apply {
        if (metadataFile.isFile) metadataFile.inputStream().buffered().use(::load)
    }

    private fun save(properties: Properties) {
        metadataFile.parentFile?.mkdirs()
        val temporaryFile = File(metadataFile.parentFile, "${metadataFile.name}.tmp")
        temporaryFile.outputStream().buffered().use { properties.store(it, null) }
        try {
            Files.move(
                temporaryFile.toPath(),
                metadataFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporaryFile.toPath(), metadataFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun descriptionKey(playlistId: String) = "playlist.$playlistId.description"
}
