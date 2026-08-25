package com.metrolist.desktop.sync

import com.metrolist.desktop.auth.DesktopAccountProfileRepository
import com.metrolist.desktop.auth.SupabaseDesktopProfileDataSource
import com.metrolist.desktop.storage.CloudinaryImageUploader
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import java.io.File
import java.time.Instant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabasePlaylistSyncRemote(
    private val client: SupabaseClient = HikalistSupabase.client,
    private val profiles: DesktopAccountProfileRepository = DesktopAccountProfileRepository(
        SupabaseDesktopProfileDataSource(client),
    ),
    private val coverUploader: CloudinaryImageUploader = CloudinaryImageUploader(),
) : PlaylistSyncRemote {
    private val realtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun ensureAuthenticated(): SyncIdentity {
        client.auth.awaitInitialization()
        val session = client.auth.currentSessionOrNull() ?: throw AuthRequiredException()
        val userId = session.user?.id ?: throw AuthRequiredException()
        val email = session.user?.email ?: throw AuthRequiredException()
        val profile = profiles.loadOrCreate(userId, email)
        return SyncIdentity(userId = userId, username = profile.username)
    }

    override suspend fun push(operation: SyncOperation, identity: SyncIdentity) {
        when (operation.entityType) {
            SyncEntityType.PLAYLIST -> pushPlaylist(operation, identity)
            SyncEntityType.SONG -> pushSong(operation, identity)
        }
    }

    override suspend fun pull(cursors: SyncCursors): RemoteSyncBatch {
        val userId = client.auth.currentSessionOrNull()?.user?.id ?: throw AuthRequiredException()
        val visiblePlaylists = client.from("hika_playlists").select()
            .decodeList<SupabasePlaylist>()
            .filter { it.deletedAt == null }
        val playlistChanges = client.from("hika_playlists").select {
            filter { gt("change_seq", cursors.playlists) }
            order("change_seq", Order.ASCENDING)
            limit(PULL_LIMIT)
        }.decodeList<SupabasePlaylist>().map { it.toRemote(userId) }

        val memberChanges = client.from("hika_playlist_members").select(
            columns = Columns.list("playlist_id", "change_seq", "deleted_at"),
        ) {
            filter { gt("change_seq", cursors.members) }
            order("change_seq", Order.ASCENDING)
            limit(PULL_LIMIT)
        }.decodeList<SupabaseMemberChange>()

        val songChanges = client.from("hika_playlist_songs").select {
            filter { gt("change_seq", cursors.songs) }
            order("change_seq", Order.ASCENDING)
            limit(PULL_LIMIT)
        }.decodeList<SupabasePlaylistSong>().map { it.toRemote(userId) }

        // A newly accepted membership can expose an older playlist whose change_seq is
        // behind the local cursor. Refresh all currently visible rows when membership changes.
        val playlists = if (memberChanges.isEmpty()) playlistChanges else {
            client.from("hika_playlists").select()
                .decodeList<SupabasePlaylist>()
                .map { it.toRemote(userId) }
        }
        val songs = if (memberChanges.isEmpty()) songChanges else {
            client.from("hika_playlist_songs").select()
                .decodeList<SupabasePlaylistSong>()
                .map { it.toRemote(userId) }
        }

        return RemoteSyncBatch(
            playlists = playlists,
            songs = songs,
            playlistCursor = playlists.maxOfOrNull(RemotePlaylist::changeSequence) ?: cursors.playlists,
            memberCursor = memberChanges.maxOfOrNull(SupabaseMemberChange::changeSequence) ?: cursors.members,
            songCursor = songs.maxOfOrNull(RemotePlaylistSong::changeSequence) ?: cursors.songs,
            visiblePlaylistIds = visiblePlaylists
                .mapTo(mutableSetOf()) { LikedSongsSync.toLocalPlaylistId(it.id, userId) },
        )
    }

    override suspend fun subscribe(onRemoteChange: suspend () -> Unit) {
        ensureAuthenticated()
        val channel = client.channel("hikalist-playlist-sync")
        val playlistChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "hika_playlists"
        }
        val memberChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "hika_playlist_members"
        }
        val songChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "hika_playlist_songs"
        }
        merge(playlistChanges, memberChanges, songChanges)
            .onEach { onRemoteChange() }
            .launchIn(realtimeScope)
        client.realtime.connect()
        channel.subscribe(blockUntilSubscribed = true)
    }

    override suspend fun close() {
        realtimeScope.cancel()
        client.realtime.disconnect()
        client.close()
    }

    private suspend fun pushPlaylist(operation: SyncOperation, identity: SyncIdentity) {
        val remotePlaylistId = LikedSongsSync.toRemotePlaylistId(operation.playlistId, identity.userId)
        if (operation.action == SyncAction.DELETE) {
            client.from("hika_playlists").update(SupabaseTombstone(Instant.now().toString())) {
                filter { eq("id", remotePlaylistId) }
            }
            return
        }
        val payload = SyncPayloadCodec.decodePlaylist(operation.payload)
        val syncedCoverUrl = uploadCoverIfNeeded(
            coverUrl = payload.coverUrl,
            playlistId = remotePlaylistId,
        )
        client.from("hika_playlists").upsert(
            SupabasePlaylist(
                id = remotePlaylistId,
                ownerId = identity.userId,
                name = payload.name,
                description = payload.description,
                coverUrl = syncedCoverUrl,
                deletedAt = payload.deletedAtEpochSeconds?.toIsoTimestamp(),
            ),
        ) {
            onConflict = "id"
        }
    }

    private suspend fun uploadCoverIfNeeded(coverUrl: String?, playlistId: String): String? {
        if (coverUrl.isNullOrBlank() || coverUrl.startsWith("https://") || coverUrl.startsWith("http://")) {
            return coverUrl
        }
        val file = File(coverUrl)
        if (!file.isFile) return null
        return coverUploader.upload(file, publicId = "$COVER_FOLDER/$playlistId").url
    }

    private suspend fun pushSong(operation: SyncOperation, identity: SyncIdentity) {
        val remotePlaylistId = LikedSongsSync.toRemotePlaylistId(operation.playlistId, identity.userId)
        if (operation.action == SyncAction.DELETE) {
            client.from("hika_playlist_songs").update(SupabaseTombstone(Instant.now().toString())) {
                filter {
                    eq("playlist_id", remotePlaylistId)
                    eq("song_id", operation.entityId)
                }
            }
            return
        }
        val payload = SyncPayloadCodec.decodeSong(operation.payload)
        client.from("hika_playlist_songs").upsert(
            SupabasePlaylistSong(
                playlistId = remotePlaylistId,
                songId = operation.entityId,
                title = payload.title,
                artist = payload.artist,
                album = payload.album,
                thumbnailUrl = payload.thumbnailUrl,
                duration = payload.duration,
                position = payload.position,
                addedBy = identity.userId,
                addedByName = payload.addedByName.takeUnless { it == "You" } ?: identity.username,
                addedAt = payload.addedAtEpochSeconds.toIsoTimestamp(),
                deletedAt = payload.deletedAtEpochSeconds?.toIsoTimestamp(),
            ),
        ) {
            onConflict = "playlist_id,song_id"
        }
    }

    private companion object {
        const val PULL_LIMIT = 500L
        const val COVER_FOLDER = "hikalist/covers"
    }
}

@Serializable
private data class SupabaseTombstone(
    @SerialName("deleted_at") val deletedAt: String,
)

@Serializable
private data class SupabasePlaylist(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String,
    @SerialName("cover_url") val coverUrl: String?,
    @SerialName("deleted_at") val deletedAt: String?,
    @SerialName("change_seq") val changeSequence: Long = 0L,
) {
    fun toRemote(currentUserId: String) = RemotePlaylist(
        id = LikedSongsSync.toLocalPlaylistId(id, currentUserId),
        name = name,
        description = description,
        coverUrl = coverUrl,
        ownerId = ownerId,
        deletedAt = deletedAt?.toEpochSeconds(),
        changeSequence = changeSequence,
    )
}

@Serializable
private data class SupabasePlaylistSong(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("song_id") val songId: String,
    val title: String,
    val artist: String,
    val album: String?,
    @SerialName("thumbnail_url") val thumbnailUrl: String?,
    val duration: Int,
    val position: Int,
    @SerialName("added_by") val addedBy: String,
    @SerialName("added_by_name") val addedByName: String,
    @SerialName("added_at") val addedAt: String,
    @SerialName("deleted_at") val deletedAt: String?,
    @SerialName("change_seq") val changeSequence: Long = 0L,
) {
    fun toRemote(currentUserId: String) = RemotePlaylistSong(
        playlistId = LikedSongsSync.toLocalPlaylistId(playlistId, currentUserId),
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        thumbnailUrl = thumbnailUrl,
        duration = duration,
        position = position,
        addedBy = addedBy,
        addedByName = addedByName,
        addedAtEpochSeconds = addedAt.toEpochSeconds(),
        deletedAt = deletedAt?.toEpochSeconds(),
        changeSequence = changeSequence,
    )
}

@Serializable
private data class SupabaseMemberChange(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("change_seq") val changeSequence: Long,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

private fun Long.toIsoTimestamp(): String = Instant.ofEpochSecond(this).toString()

private fun String.toEpochSeconds(): Long = Instant.parse(this).epochSecond
