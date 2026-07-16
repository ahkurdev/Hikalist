package com.metrolist.desktop.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class PlaylistCollaborationRepositoryTest {
    @Test
    fun `owner can manage playlist while editor can only edit songs`() {
        val access = PlaylistCollaborationAccess(
            ownerId = "owner",
            currentUserId = "editor",
            collaborators = listOf(
                PlaylistCollaborator("owner", "Allan", null, PlaylistMemberRole.OWNER),
                PlaylistCollaborator("editor", "Friend", null, PlaylistMemberRole.EDITOR),
            ),
        )

        assertFalse(access.canManagePlaylist)
        assertTrue(access.canEditSongs)
        assertEquals(PlaylistMemberRole.EDITOR, access.currentRole)
    }

    @Test
    fun `adding an existing editor restores membership`() = kotlinx.coroutines.runBlocking {
        val dataSource = FakeCollaborationDataSource(
            ownerId = "owner",
            profiles = listOf(PlaylistCollaboratorProfile("editor", "Friend", null)),
            members = mutableListOf(
                PlaylistMemberRecord("editor", PlaylistMemberRole.EDITOR, deleted = true),
            ),
        )
        val repository = PlaylistCollaborationRepository(dataSource)

        val invitation = repository.addEditor("playlist 1", " Friend ", "owner")

        assertEquals("editor", invitation.collaborator.userId)
        assertEquals(PlaylistMemberRole.EDITOR, invitation.collaborator.role)
        assertEquals(1, dataSource.members.size)
        assertFalse(dataSource.members.single().deleted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `ambiguous username is rejected instead of inviting wrong account`() {
        kotlinx.coroutines.runBlocking {
            val dataSource = FakeCollaborationDataSource(
                ownerId = "owner",
                profiles = listOf(
                    PlaylistCollaboratorProfile("one", "Same", null),
                    PlaylistCollaboratorProfile("two", "Same", null),
                ),
            )

            PlaylistCollaborationRepository(dataSource).addEditor("playlist", "Same", "owner")
        }
    }

    @Test
    fun `owner creates a two hour editor invite without exposing playlist id`() = kotlinx.coroutines.runBlocking {
        val dataSource = FakeCollaborationDataSource(ownerId = "owner")
        val repository = PlaylistCollaborationRepository(dataSource)

        val invite = repository.createEditorInviteLink("private playlist", "owner")

        assertEquals("https://web-hikalist.vercel.app/invite?token=secret-token", invite.shareUrl)
        assertEquals(Instant.parse("2026-07-16T05:00:00Z"), invite.expiresAt)
        assertEquals(7_200, dataSource.lastInviteTtlSeconds)
        assertFalse(invite.shareUrl.contains("private playlist"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `editor cannot create an invite link`() {
        kotlinx.coroutines.runBlocking {
            PlaylistCollaborationRepository(FakeCollaborationDataSource(ownerId = "owner"))
                .createEditorInviteLink("playlist", "editor")
        }
    }

    private class FakeCollaborationDataSource(
        private val ownerId: String,
        private val profiles: List<PlaylistCollaboratorProfile> = emptyList(),
        val members: MutableList<PlaylistMemberRecord> = mutableListOf(),
    ) : PlaylistCollaborationDataSource {
        var lastInviteTtlSeconds: Int? = null

        override suspend fun ownerId(playlistId: String): String = ownerId

        override suspend fun members(playlistId: String): List<PlaylistMemberRecord> = members.toList()

        override suspend fun profiles(userIds: Set<String>): List<PlaylistCollaboratorProfile> =
            profiles.filter { it.userId in userIds }

        override suspend fun findProfilesByUsername(username: String): List<PlaylistCollaboratorProfile> =
            profiles.filter { it.username.equals(username, ignoreCase = true) }

        override suspend fun upsertEditor(playlistId: String, userId: String) {
            members.removeAll { it.userId == userId }
            members += PlaylistMemberRecord(userId, PlaylistMemberRole.EDITOR, deleted = false)
        }

        override suspend fun removeMember(playlistId: String, userId: String) {
            members.removeAll { it.userId == userId }
        }

        override suspend fun createEditorInvite(playlistId: String, ttlSeconds: Int): PlaylistInviteToken {
            lastInviteTtlSeconds = ttlSeconds
            return PlaylistInviteToken("secret-token", Instant.parse("2026-07-16T05:00:00Z"))
        }

        override suspend fun revokeEditorInvites(playlistId: String) = Unit
    }
}
