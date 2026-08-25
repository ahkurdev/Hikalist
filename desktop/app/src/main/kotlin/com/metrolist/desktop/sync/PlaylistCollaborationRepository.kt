package com.metrolist.desktop.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class PlaylistMemberRole { OWNER, EDITOR }

data class PlaylistCollaborator(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
    val role: PlaylistMemberRole,
)

data class PlaylistCollaborationAccess(
    val ownerId: String,
    val currentUserId: String,
    val collaborators: List<PlaylistCollaborator>,
) {
    val currentRole: PlaylistMemberRole? = collaborators.firstOrNull { it.userId == currentUserId }?.role
    val canManagePlaylist: Boolean = currentUserId == ownerId
    val canEditSongs: Boolean = canManagePlaylist || currentRole == PlaylistMemberRole.EDITOR
}

data class PlaylistInvitation(
    val collaborator: PlaylistCollaborator,
)

data class PlaylistInviteToken(val token: String, val expiresAt: Instant)

data class PlaylistInviteLink(val shareUrl: String, val expiresAt: Instant)

data class PlaylistCollaboratorProfile(
    val userId: String,
    val username: String,
    val avatarUrl: String?,
)

data class PlaylistMemberRecord(
    val userId: String,
    val role: PlaylistMemberRole,
    val deleted: Boolean,
)

interface PlaylistCollaborationDataSource {
    suspend fun ownerId(playlistId: String): String
    suspend fun members(playlistId: String): List<PlaylistMemberRecord>
    suspend fun profiles(userIds: Set<String>): List<PlaylistCollaboratorProfile>
    suspend fun findProfilesByUsername(username: String): List<PlaylistCollaboratorProfile>
    suspend fun upsertEditor(playlistId: String, userId: String)
    suspend fun removeMember(playlistId: String, userId: String)
    suspend fun createEditorInvite(playlistId: String, ttlSeconds: Int): PlaylistInviteToken
    suspend fun revokeEditorInvites(playlistId: String)
}

class PlaylistCollaborationRepository(
    private val dataSource: PlaylistCollaborationDataSource = SupabasePlaylistCollaborationDataSource(),
    private val accountUrl: String = System.getenv("HIKALIST_ACCOUNT_URL")
        ?.takeIf(String::isNotBlank)
        ?: "https://hikalist.ahkur.my.id",
) {
    suspend fun access(playlistId: String, currentUserId: String): PlaylistCollaborationAccess {
        val ownerId = dataSource.ownerId(playlistId)
        val members = dataSource.members(playlistId).filterNot(PlaylistMemberRecord::deleted)
        val profiles = dataSource.profiles(members.mapTo(mutableSetOf(), PlaylistMemberRecord::userId) + ownerId)
            .associateBy(PlaylistCollaboratorProfile::userId)
        val collaborators = buildList {
            val owner = profiles[ownerId]
            add(PlaylistCollaborator(ownerId, owner?.username ?: "Playlist owner", owner?.avatarUrl, PlaylistMemberRole.OWNER))
            members.filter { it.userId != ownerId }.forEach { member ->
                val profile = profiles[member.userId]
                add(
                    PlaylistCollaborator(
                        userId = member.userId,
                        username = profile?.username ?: "Hikalist user",
                        avatarUrl = profile?.avatarUrl,
                        role = member.role,
                    ),
                )
            }
        }
        return PlaylistCollaborationAccess(ownerId, currentUserId, collaborators)
    }

    suspend fun addEditor(playlistId: String, username: String, currentUserId: String): PlaylistInvitation {
        require(dataSource.ownerId(playlistId) == currentUserId) { "Only the playlist owner can invite editors" }
        val normalized = username.trim()
        require(normalized.isNotEmpty()) { "Enter a Hikalist username" }
        val matches = dataSource.findProfilesByUsername(normalized)
            .filter { it.username.equals(normalized, ignoreCase = true) }
        require(matches.size == 1) {
            if (matches.isEmpty()) "Hikalist username not found" else "More than one account uses that username"
        }
        val profile = matches.single()
        require(profile.userId != currentUserId) { "The owner is already in this playlist" }
        dataSource.upsertEditor(playlistId, profile.userId)
        return PlaylistInvitation(
            collaborator = PlaylistCollaborator(
                profile.userId,
                profile.username,
                profile.avatarUrl,
                PlaylistMemberRole.EDITOR,
            ),
        )
    }

    suspend fun createEditorInviteLink(playlistId: String, currentUserId: String): PlaylistInviteLink {
        require(dataSource.ownerId(playlistId) == currentUserId) { "Only the playlist owner can create invite links" }
        val invite = dataSource.createEditorInvite(playlistId, INVITE_TTL_SECONDS)
        return PlaylistInviteLink(
            shareUrl = "$accountUrl/invite?token=${URLEncoder.encode(invite.token, StandardCharsets.UTF_8)}",
            expiresAt = invite.expiresAt,
        )
    }

    suspend fun revokeEditorInviteLinks(playlistId: String, currentUserId: String) {
        require(dataSource.ownerId(playlistId) == currentUserId) { "Only the playlist owner can revoke invite links" }
        dataSource.revokeEditorInvites(playlistId)
    }

    suspend fun removeEditor(playlistId: String, userId: String, currentUserId: String) {
        require(dataSource.ownerId(playlistId) == currentUserId) { "Only the playlist owner can remove editors" }
        require(userId != currentUserId) { "The playlist owner cannot be removed" }
        dataSource.removeMember(playlistId, userId)
    }

    private companion object {
        const val INVITE_TTL_SECONDS = 2 * 60 * 60
    }
}

internal class SupabasePlaylistCollaborationDataSource(
    private val client: SupabaseClient = HikalistSupabase.client,
) : PlaylistCollaborationDataSource {
    override suspend fun ownerId(playlistId: String): String = client.from(PLAYLISTS).select(
        columns = Columns.list("owner_id"),
    ) {
        filter { eq("id", playlistId) }
        limit(1)
    }.decodeList<SupabaseOwner>().firstOrNull()?.ownerId
        ?: throw IllegalArgumentException("Playlist is not available")

    override suspend fun members(playlistId: String): List<PlaylistMemberRecord> = client.from(MEMBERS).select {
        filter { eq("playlist_id", playlistId) }
    }.decodeList<SupabaseMember>().map { member ->
        PlaylistMemberRecord(
            userId = member.userId,
            role = if (member.role.equals("owner", true)) PlaylistMemberRole.OWNER else PlaylistMemberRole.EDITOR,
            deleted = member.deletedAt != null,
        )
    }

    override suspend fun profiles(userIds: Set<String>): List<PlaylistCollaboratorProfile> =
        if (userIds.isEmpty()) emptyList() else allProfiles().filter { it.userId in userIds }

    override suspend fun findProfilesByUsername(username: String): List<PlaylistCollaboratorProfile> =
        allProfiles().filter { it.username.equals(username, ignoreCase = true) }

    override suspend fun upsertEditor(playlistId: String, userId: String) {
        val existing = members(playlistId).firstOrNull { it.userId == userId }
        if (existing?.deleted == false) return
        if (existing != null) {
            client.from(MEMBERS).delete {
                filter {
                    eq("playlist_id", playlistId)
                    eq("user_id", userId)
                }
            }
        }
        client.from(MEMBERS).insert(
            SupabaseMemberMutation(playlistId = playlistId, userId = userId, role = "editor"),
        )
    }

    override suspend fun removeMember(playlistId: String, userId: String) {
        client.from(MEMBERS).update(
            SupabaseMemberDeletion(deletedAt = Instant.now().toString()),
        ) {
            filter {
                eq("playlist_id", playlistId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun createEditorInvite(playlistId: String, ttlSeconds: Int): PlaylistInviteToken {
        val response = client.postgrest.rpc(
            "create_hika_playlist_invite",
            SupabaseCreateInviteRequest(playlistId, ttlSeconds),
        ).decodeSingle<SupabaseInviteResponse>()
        return PlaylistInviteToken(response.token, Instant.parse(response.expiresAt))
    }

    override suspend fun revokeEditorInvites(playlistId: String) {
        client.postgrest.rpc(
            "revoke_hika_playlist_invites",
            SupabasePlaylistRequest(playlistId),
        )
    }

    private suspend fun allProfiles(): List<PlaylistCollaboratorProfile> = client.from(PROFILES).select(
        columns = Columns.list("user_id", "username", "avatar_url"),
    ).decodeList<SupabaseCollaboratorProfile>().map(SupabaseCollaboratorProfile::toProfile)

    private companion object {
        const val PLAYLISTS = "hika_playlists"
        const val MEMBERS = "hika_playlist_members"
        const val PROFILES = "hika_profiles"
    }
}

@Serializable
private data class SupabaseOwner(@SerialName("owner_id") val ownerId: String)

@Serializable
private data class SupabaseMember(
    @SerialName("user_id") val userId: String,
    val role: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
private data class SupabaseCollaboratorProfile(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    fun toProfile() = PlaylistCollaboratorProfile(userId, username, avatarUrl)
}

@Serializable
private data class SupabaseMemberMutation(
    @SerialName("playlist_id") val playlistId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
)

@Serializable
private data class SupabaseMemberDeletion(@SerialName("deleted_at") val deletedAt: String)

@Serializable
private data class SupabaseCreateInviteRequest(
    @SerialName("p_playlist_id") val playlistId: String,
    @SerialName("p_ttl_seconds") val ttlSeconds: Int,
)

@Serializable
private data class SupabasePlaylistRequest(@SerialName("p_playlist_id") val playlistId: String)

@Serializable
private data class SupabaseInviteResponse(
    val token: String,
    @SerialName("expires_at") val expiresAt: String,
)
