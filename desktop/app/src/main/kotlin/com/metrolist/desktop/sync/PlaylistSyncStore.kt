package com.metrolist.desktop.sync

import java.io.File
import java.sql.DriverManager
import kotlin.math.min

class PlaylistSyncStore(
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
                    CREATE TABLE IF NOT EXISTS sync_outbox (
                        id TEXT PRIMARY KEY,
                        entity_type TEXT NOT NULL,
                        action TEXT NOT NULL,
                        playlist_id TEXT NOT NULL,
                        entity_id TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        attempt_count INTEGER NOT NULL DEFAULT 0,
                        next_attempt_at INTEGER NOT NULL,
                        last_error TEXT
                    )
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE INDEX IF NOT EXISTS sync_outbox_ready_idx
                    ON sync_outbox(next_attempt_at, created_at)
                    """.trimIndent(),
                )
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS sync_state (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }
    }

    @Synchronized
    fun enqueue(operation: SyncOperation) {
        val now = nowEpochSeconds()
        connect().use { connection ->
            connection.autoCommit = false
            val isPlaylistDelete = operation.entityType == SyncEntityType.PLAYLIST &&
                operation.action == SyncAction.DELETE
            if (isPlaylistDelete) {
                connection.prepareStatement(
                    "DELETE FROM sync_outbox WHERE playlist_id = ?",
                ).use { statement ->
                    statement.setString(1, operation.playlistId)
                    statement.executeUpdate()
                }
            } else {
                connection.prepareStatement(
                    """
                    DELETE FROM sync_outbox
                    WHERE entity_type = ? AND playlist_id = ? AND entity_id = ?
                    """.trimIndent(),
                ).use { statement ->
                    statement.setString(1, operation.entityType.name)
                    statement.setString(2, operation.playlistId)
                    statement.setString(3, operation.entityId)
                    statement.executeUpdate()
                }
            }
            connection.prepareStatement(
                """
                INSERT OR IGNORE INTO sync_outbox (
                    id, entity_type, action, playlist_id, entity_id, payload,
                    created_at, attempt_count, next_attempt_at, last_error
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, NULL)
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, operation.id)
                statement.setString(2, operation.entityType.name)
                statement.setString(3, operation.action.name)
                statement.setString(4, operation.playlistId)
                statement.setString(5, operation.entityId)
                statement.setString(6, operation.payload)
                statement.setLong(7, now)
                statement.setLong(8, now)
                statement.executeUpdate()
            }
            connection.commit()
        }
    }

    @Synchronized
    fun pending(limit: Int = 50): List<SyncOperation> = connect().use { connection ->
        connection.prepareStatement(
            """
            SELECT id, entity_type, action, playlist_id, entity_id, payload,
                   attempt_count, last_error
            FROM sync_outbox
            WHERE next_attempt_at <= ?
            ORDER BY created_at,
                     CASE entity_type WHEN 'PLAYLIST' THEN 0 ELSE 1 END,
                     id
            LIMIT ?
            """.trimIndent(),
        ).use { statement ->
            statement.setLong(1, nowEpochSeconds())
            statement.setInt(2, limit.coerceIn(1, 500))
            statement.executeQuery().use { result ->
                buildList {
                    while (result.next()) {
                        add(
                            SyncOperation(
                                id = result.getString("id"),
                                entityType = SyncEntityType.valueOf(result.getString("entity_type")),
                                action = SyncAction.valueOf(result.getString("action")),
                                playlistId = result.getString("playlist_id"),
                                entityId = result.getString("entity_id"),
                                payload = result.getString("payload"),
                                attemptCount = result.getInt("attempt_count"),
                                lastError = result.getString("last_error"),
                            ),
                        )
                    }
                }
            }
        }
    }

    @Synchronized
    fun markSucceeded(operationId: String) {
        connect().use { connection ->
            connection.prepareStatement("DELETE FROM sync_outbox WHERE id = ?").use { statement ->
                statement.setString(1, operationId)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun markFailed(operationId: String, error: String) {
        connect().use { connection ->
            val attempts = connection.prepareStatement(
                "SELECT attempt_count FROM sync_outbox WHERE id = ?",
            ).use { statement ->
                statement.setString(1, operationId)
                statement.executeQuery().use { result -> if (result.next()) result.getInt(1) else return }
            }
            val nextAttempts = attempts + 1
            val retryDelay = min(300L, 1L shl nextAttempts.coerceAtMost(8))
            connection.prepareStatement(
                """
                UPDATE sync_outbox
                SET attempt_count = ?, next_attempt_at = ?, last_error = ?
                WHERE id = ?
                """.trimIndent(),
            ).use { statement ->
                statement.setInt(1, nextAttempts)
                statement.setLong(2, nowEpochSeconds() + retryDelay)
                statement.setString(3, error.take(500))
                statement.setString(4, operationId)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun countPending(): Int = connect().use { connection ->
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM sync_outbox").use { result ->
                if (result.next()) result.getInt(1) else 0
            }
        }
    }

    @Synchronized
    fun saveCursor(cursor: SyncCursor, value: Long) {
        saveState(cursor.storageKey, value.coerceAtLeast(0L).toString())
    }

    @Synchronized
    fun saveState(key: String, value: String) {
        connect().use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO sync_state(key, value) VALUES (?, ?)
                ON CONFLICT(key) DO UPDATE SET value = excluded.value
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, key)
                statement.setString(2, value)
                statement.executeUpdate()
            }
        }
    }

    @Synchronized
    fun loadState(key: String): String? = connect().use { connection ->
        connection.prepareStatement("SELECT value FROM sync_state WHERE key = ?").use { statement ->
            statement.setString(1, key)
            statement.executeQuery().use { result ->
                if (result.next()) result.getString(1) else null
            }
        }
    }

    fun loadCursor(cursor: SyncCursor): Long =
        loadState(cursor.storageKey)?.toLongOrNull() ?: 0L

    fun loadCursors() = SyncCursors(
        playlists = loadCursor(SyncCursor.PLAYLISTS),
        members = loadCursor(SyncCursor.MEMBERS),
        songs = loadCursor(SyncCursor.SONGS),
    )

    @Synchronized
    fun prepareForUser(userId: String) {
        if (loadState(ACTIVE_USER_KEY) == userId) return
        SyncCursor.entries.forEach { saveCursor(it, 0L) }
        saveState(ACTIVE_USER_KEY, userId)
    }

    private fun connect() = DriverManager.getConnection(jdbcUrl)

    private companion object {
        const val ACTIVE_USER_KEY = "active_sync_user"
    }
}
