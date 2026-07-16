package com.metrolist.desktop.ui

import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.sync.PlaylistCollaborationAccess
import com.metrolist.desktop.sync.PlaylistMemberRole

internal fun playlistStatusLabel(
    playlist: PlaylistRow,
    access: PlaylistCollaborationAccess?,
): String {
    if (access == null) return if (playlist.isLocal) "Local playlist" else "Synced playlist"

    val kind = if (access.collaborators.size > 1) "Collaborative playlist" else "Synced playlist"
    val role = when (access.currentRole) {
        PlaylistMemberRole.OWNER -> "Owner"
        PlaylistMemberRole.EDITOR -> "Editor"
        null -> null
    }
    return listOfNotNull(kind, role).joinToString(" • ")
}
