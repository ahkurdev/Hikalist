package com.metrolist.desktop.lyrics

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Base64

class LyricsCacheStore(
    private val directory: File = File(System.getProperty("user.home"), ".metrolist/lyrics"),
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
) {
    fun read(videoId: String): String? = runCatching {
        val file = fileFor(videoId)
        if (!file.isFile) return@runCatching null
        file.setLastModified(System.currentTimeMillis())
        file.readText(StandardCharsets.UTF_8)
    }.getOrNull()

    fun write(videoId: String, lyrics: String) {
        runCatching {
            directory.mkdirs()
            val destination = fileFor(videoId)
            val temporary = Files.createTempFile(directory.toPath(), destination.nameWithoutExtension, ".tmp")
            try {
                Files.writeString(temporary, lyrics, StandardCharsets.UTF_8)
                runCatching {
                    Files.move(
                        temporary,
                        destination.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }.getOrElse {
                    Files.move(temporary, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                Files.deleteIfExists(temporary)
            }
            pruneOldEntries()
        }
    }

    fun remove(videoId: String) {
        runCatching { Files.deleteIfExists(fileFor(videoId).toPath()) }
    }

    private fun pruneOldEntries() {
        if (maxEntries < 1) return
        directory.listFiles { file -> file.isFile && file.extension == CACHE_EXTENSION }
            .orEmpty()
            .sortedByDescending(File::lastModified)
            .drop(maxEntries)
            .forEach { file -> runCatching { file.delete() } }
    }

    private fun fileFor(videoId: String): File {
        val encodedId = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(videoId.toByteArray(StandardCharsets.UTF_8))
        return directory.resolve("$encodedId.$CACHE_EXTENSION")
    }

    companion object {
        private const val CACHE_EXTENSION = "lrc"
        private const val DEFAULT_MAX_ENTRIES = 500
    }
}
