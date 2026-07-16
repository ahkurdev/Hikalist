package com.metrolist.desktop.sync

import java.util.UUID
import java.nio.file.Files
import java.util.Base64
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class SupabasePlaylistSyncProbeTest {
    @Test
    fun `authenticated client can round trip a playlist and song through Supabase`() = runBlocking {
        assumeTrue(System.getenv("HIKALIST_SUPABASE_PROBE") == "1")
        val accessToken = System.getenv("HIKALIST_SUPABASE_ACCESS_TOKEN")
        val refreshToken = System.getenv("HIKALIST_SUPABASE_REFRESH_TOKEN")
        assumeTrue(!accessToken.isNullOrBlank() && !refreshToken.isNullOrBlank())
        val client = createHikalistSupabaseClient()
        client.auth.importAuthToken(accessToken, refreshToken, retrieveUser = true, autoRefresh = true)
        val remote = SupabasePlaylistSyncRemote(client)
        val identity = remote.ensureAuthenticated()
        val playlistId = "probe-${UUID.randomUUID()}"
        val songId = "probe-song-${UUID.randomUUID()}"
        val coverFile = Files.createTempFile("hikalist-cover-probe", ".png").toFile().apply {
            writeBytes(Base64.getDecoder().decode(TINY_PNG))
            deleteOnExit()
        }
        val playlist = SyncOperation(
            id = UUID.randomUUID().toString(),
            entityType = SyncEntityType.PLAYLIST,
            action = SyncAction.UPSERT,
            playlistId = playlistId,
            entityId = playlistId,
            payload = SyncPayloadCodec.encode(
                PlaylistSyncPayload("Sync probe", "", coverFile.absolutePath, null),
            ),
        )
        val song = SyncOperation(
            id = UUID.randomUUID().toString(),
            entityType = SyncEntityType.SONG,
            action = SyncAction.UPSERT,
            playlistId = playlistId,
            entityId = songId,
            payload = SyncPayloadCodec.encode(
                SongSyncPayload("Probe song", "Hikalist", null, null, 1, 0, "You", 100L, null),
            ),
        )
        try {
            remote.push(playlist, identity)
            remote.push(song, identity)

            val batch = remote.pull(SyncCursors())
            assertTrue(
                batch.playlists.any {
                    it.id == playlistId &&
                        it.deletedAt == null &&
                        it.coverUrl?.startsWith("${HikalistSupabaseConfig.PROJECT_URL}/storage/v1/object/public/playlist-covers/") == true
                },
            )
            assertTrue(batch.songs.any { it.playlistId == playlistId && it.songId == songId && it.deletedAt == null })
        } finally {
            remote.push(song.copy(action = SyncAction.DELETE, payload = "{}"), identity)
            remote.push(playlist.copy(action = SyncAction.DELETE, payload = "{}"), identity)
            remote.close()
            client.close()
        }
    }

    private companion object {
        const val TINY_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="
    }
}
