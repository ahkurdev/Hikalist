package com.metrolist.desktop.playlist

import java.io.File
import java.sql.DriverManager
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PlaylistSongMetadata(
    val playlistId: String,
    val songId: String,
    val addedBy: String,
    val addedAtEpochSeconds: Long,
)

class PlaylistSongMetadataStore(
    private val databaseFile: File = File(
        System.getProperty("user.home"),
        ".metrolist/playlist-song-metadata.db",
    ),
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
) {
    private val jdbcUrl: String

    init {
        databaseFile.parentFile?.mkdirs()
        jdbcUrl = "jdbc:sqlite:${databaseFile.canonicalPath}"
        connect().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS playlist_song_metadata (
                        playlist_id TEXT NOT NULL,
                        song_id TEXT NOT NULL,
                        added_by TEXT NOT NULL,
                        added_at INTEGER NOT NULL,
                        PRIMARY KEY (playlist_id, song_id)
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    @Synchronized
    fun recordIfAbsent(
        playlistId: String,
        songId: String,
        addedBy: String,
        addedAtEpochSeconds: Long = nowEpochSeconds(),
    ) {
        connect().use { connection ->
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO playlist_song_metadata
                    (playlist_id, song_id, added_by, added_at)
                VALUES (?, ?, ?, ?)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, playlistId)
                statement.setString(2, songId)
                statement.setString(3, addedBy.trim().ifBlank { LOCAL_CONTRIBUTOR })
                statement.setLong(4, addedAtEpochSeconds)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun upsert(
        playlistId: String,
        songId: String,
        addedBy: String,
        addedAtEpochSeconds: Long,
    ) {
        connect().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO playlist_song_metadata (playlist_id, song_id, added_by, added_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(playlist_id, song_id) DO UPDATE SET
                    added_by = excluded.added_by,
                    added_at = excluded.added_at
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, playlistId)
                statement.setString(2, songId)
                statement.setString(3, addedBy.trim().ifBlank { LOCAL_CONTRIBUTOR })
                statement.setLong(4, addedAtEpochSeconds)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun getForPlaylist(playlistId: String): Map<String, PlaylistSongMetadata> =
        connect().use { connection ->
            connection.prepareStatement(
                """
                SELECT song_id, added_by, added_at
                FROM playlist_song_metadata
                WHERE playlist_id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, playlistId)
                statement.executeQuery().use { result ->
                    buildMap {
                        while (result.next()) {
                            val songId = result.getString("song_id")
                            put(
                                songId,
                                PlaylistSongMetadata(
                                    playlistId = playlistId,
                                    songId = songId,
                                    addedBy = result.getString("added_by"),
                                    addedAtEpochSeconds = result.getLong("added_at"),
                                ),
                            )
                        }
                    }
                }
            }
        }

    @Synchronized
    fun remove(playlistId: String, songId: String) {
        connect().use { connection ->
            connection.prepareStatement(
                "DELETE FROM playlist_song_metadata WHERE playlist_id = ? AND song_id = ?",
            ).use { statement ->
                statement.setString(1, playlistId)
                statement.setString(2, songId)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun deletePlaylist(playlistId: String) {
        connect().use { connection ->
            connection.prepareStatement(
                "DELETE FROM playlist_song_metadata WHERE playlist_id = ?",
            ).use { statement ->
                statement.setString(1, playlistId)
                statement.executeUpdate()
            }
        }
    }

    private fun connect() = DriverManager.getConnection(jdbcUrl)

    companion object {
        const val LOCAL_CONTRIBUTOR = "You"
    }
}

object PlaylistSongMetadataFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    fun addedBy(metadata: PlaylistSongMetadata?): String = metadata?.addedBy?.takeIf(String::isNotBlank) ?: "—"

    fun date(
        metadata: PlaylistSongMetadata?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String = metadata?.let {
        dateFormatter.withZone(zoneId).format(Instant.ofEpochSecond(it.addedAtEpochSeconds))
    } ?: "—"
}
