package com.metrolist.desktop.player

import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class QueueSessionItem(
    val videoId: String,
    val playlistId: String?,
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val duration: Int,
    val addedBy: String?,
    val addedAt: Long?,
)

@Serializable
data class QueueSessionSnapshot(
    val items: List<QueueSessionItem>,
    val currentIndex: Int,
    val positionSeconds: Double,
    val shuffled: Boolean,
    val repeatMode: RepeatMode,
)

class QueueSessionStore(
    private val file: File = File(System.getProperty("user.home"), ".metrolist/queue-session.json"),
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Synchronized
    fun load(): QueueSessionSnapshot? = runCatching {
        if (!file.isFile) return@runCatching null
        json.decodeFromString<QueueSessionSnapshot>(file.readText())
            .takeIf { it.items.isNotEmpty() && it.currentIndex in it.items.indices }
    }.getOrNull()

    @Synchronized
    fun save(snapshot: QueueSessionSnapshot) {
        if (snapshot.items.isEmpty()) {
            file.delete()
            return
        }
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeToString(QueueSessionSnapshot.serializer(), snapshot))
        try {
            Files.move(
                temporary.toPath(),
                file.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

internal fun DesktopMusicPlayer.QueueItem.toSessionItem() = QueueSessionItem(
    videoId = videoId,
    playlistId = playlistId,
    title = title,
    artist = artist,
    album = album,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    addedBy = addedBy,
    addedAt = addedAt,
)

internal fun QueueSessionItem.toQueueItem() = DesktopMusicPlayer.QueueItem(
    videoId = videoId,
    playlistId = playlistId,
    title = title,
    artist = artist,
    album = album,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    addedBy = addedBy,
    addedAt = addedAt,
)
