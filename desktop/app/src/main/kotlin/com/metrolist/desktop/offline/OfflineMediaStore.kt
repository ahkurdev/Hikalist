package com.metrolist.desktop.offline

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

data class OfflineSongMetadata(
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
)

data class OfflineMedia(
    val metadata: OfflineSongMetadata,
    val audioFile: File,
    val coverFile: File?,
    val lyricsAvailable: Boolean,
)

class OfflineMediaStore(
    private val rootDirectory: File = File(System.getProperty("user.home"), ".metrolist/offline"),
) {
    private val mediaCache = ConcurrentHashMap<String, OfflineMedia>()

    fun find(videoId: String): OfflineMedia? {
        mediaCache[videoId]?.let { cached ->
            if (cached.audioFile.isFile && cached.audioFile.length() > 0L) return cached
            mediaCache.remove(videoId)
        }
        val directory = songDirectory(videoId)
        val audio = directory.resolve(AUDIO_FILE)
        val metadataFile = directory.resolve(METADATA_FILE)
        if (!audio.isFile || audio.length() <= 0L || !metadataFile.isFile) return null

        val properties = Properties().apply {
            metadataFile.inputStream().buffered().use(::load)
        }
        if (properties.getProperty("videoId") != videoId) return null
        val metadata = OfflineSongMetadata(
            videoId = videoId,
            title = properties.getProperty("title").orEmpty(),
            artist = properties.getProperty("artist").orEmpty(),
            album = properties.getProperty("album")?.takeIf(String::isNotBlank),
            thumbnailUrl = properties.getProperty("thumbnailUrl")?.takeIf(String::isNotBlank),
            durationSeconds = properties.getProperty("durationSeconds")?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
        )
        return OfflineMedia(
            metadata = metadata,
            audioFile = audio,
            coverFile = directory.resolve(COVER_FILE).takeIf(File::isFile),
            lyricsAvailable = properties.getProperty("lyricsAvailable").toBoolean(),
        ).also { mediaCache[videoId] = it }
    }

    fun isDownloaded(videoId: String): Boolean = find(videoId) != null

    fun temporaryAudioFile(videoId: String): File = songDirectory(videoId).resolve("$AUDIO_FILE.part")

    fun temporaryCoverFile(videoId: String): File = songDirectory(videoId).resolve("$COVER_FILE.part")

    fun commit(
        metadata: OfflineSongMetadata,
        temporaryAudio: File,
        lyricsAvailable: Boolean,
        temporaryCover: File? = null,
    ): OfflineMedia {
        require(temporaryAudio.isFile && temporaryAudio.length() > 0L) { "Downloaded audio is empty" }
        val directory = songDirectory(metadata.videoId)
        directory.mkdirs()
        require(temporaryAudio.canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())) {
            "Temporary audio is outside the offline song directory"
        }

        atomicMove(temporaryAudio, directory.resolve(AUDIO_FILE))
        temporaryCover?.takeIf { it.isFile && it.length() > 0L }?.let {
            require(it.canonicalFile.toPath().startsWith(directory.canonicalFile.toPath())) {
                "Temporary cover is outside the offline song directory"
            }
            atomicMove(it, directory.resolve(COVER_FILE))
        }

        val properties = Properties().apply {
            setProperty("videoId", metadata.videoId)
            setProperty("title", metadata.title)
            setProperty("artist", metadata.artist)
            metadata.album?.let { setProperty("album", it) }
            metadata.thumbnailUrl?.let { setProperty("thumbnailUrl", it) }
            setProperty("durationSeconds", metadata.durationSeconds.toString())
            setProperty("lyricsAvailable", lyricsAvailable.toString())
        }
        val temporaryMetadata = directory.resolve("$METADATA_FILE.part")
        temporaryMetadata.outputStream().buffered().use { properties.store(it, null) }
        atomicMove(temporaryMetadata, directory.resolve(METADATA_FILE))
        return requireNotNull(find(metadata.videoId))
    }

    fun remove(videoId: String) {
        mediaCache.remove(videoId)
        val root = rootDirectory.canonicalFile
        val directory = songDirectory(videoId).canonicalFile
        require(directory.toPath().startsWith(root.toPath()) && directory != root) {
            "Invalid offline song directory"
        }
        if (directory.exists()) directory.walkBottomUp().forEach(File::delete)
    }

    internal fun songDirectory(videoId: String): File = rootDirectory.resolve(sha256(videoId))

    private fun atomicMove(source: File, destination: File) {
        destination.parentFile?.mkdirs()
        try {
            Files.move(
                source.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private companion object {
        const val AUDIO_FILE = "audio.m4a"
        const val COVER_FILE = "cover.jpg"
        const val METADATA_FILE = "metadata.properties"
    }
}
