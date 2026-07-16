package com.metrolist.desktop.ui

import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.sync.PlaylistCollaborationAccess
import com.metrolist.desktop.sync.PlaylistCollaborator
import com.metrolist.desktop.sync.PlaylistMemberRole
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistStatusLabelTest {
    @Test
    fun `local playlist without cloud access stays local`() {
        assertEquals(
            "Local playlist",
            playlistStatusLabel(PlaylistRow(id = "local", name = "Local", isLocal = true), null),
        )
    }

    @Test
    fun `cloud owner without another member is shown as synced`() {
        assertEquals(
            "Synced playlist • Owner",
            playlistStatusLabel(
                PlaylistRow(id = "cloud", name = "Cloud", isLocal = true),
                access(currentUserId = "owner", editorId = null),
            ),
        )
    }

    @Test
    fun `playlist with another member is shown as collaborative for owner`() {
        assertEquals(
            "Collaborative playlist • Owner",
            playlistStatusLabel(
                PlaylistRow(id = "collab", name = "Collab", isLocal = true),
                access(currentUserId = "owner", editorId = "editor"),
            ),
        )
    }

    @Test
    fun `invited user sees collaborative editor role`() {
        assertEquals(
            "Collaborative playlist • Editor",
            playlistStatusLabel(
                PlaylistRow(id = "collab", name = "Collab", isLocal = false),
                access(currentUserId = "editor", editorId = "editor"),
            ),
        )
    }

    private fun access(currentUserId: String, editorId: String?): PlaylistCollaborationAccess {
        val collaborators = buildList {
            add(PlaylistCollaborator("owner", "owner", null, PlaylistMemberRole.OWNER))
            editorId?.let { add(PlaylistCollaborator(it, "editor", null, PlaylistMemberRole.EDITOR)) }
        }
        return PlaylistCollaborationAccess(
            ownerId = "owner",
            currentUserId = currentUserId,
            collaborators = collaborators,
        )
    }
}
