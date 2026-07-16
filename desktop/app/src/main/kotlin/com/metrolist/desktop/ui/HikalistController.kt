package com.metrolist.desktop.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.metrolist.desktop.auth.DesktopAccountStatus
import com.metrolist.desktop.auth.DesktopAuthManager
import com.metrolist.desktop.auth.DesktopAuthMode
import com.metrolist.desktop.db.repository.DataRepository
import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.db.repository.SongRow
import com.metrolist.desktop.lyrics.LyricsResolver
import com.metrolist.desktop.offline.OfflineDownloadManager
import com.metrolist.desktop.offline.OfflineMediaStore
import com.metrolist.desktop.personalization.HomePersonalization
import com.metrolist.desktop.personalization.HomeSeed
import com.metrolist.desktop.personalization.PersonalizationSignals
import com.metrolist.desktop.player.DesktopMusicPlayer
import com.metrolist.desktop.playlist.PlaylistMetadataStore
import com.metrolist.desktop.playlist.PlaylistSongMetadata
import com.metrolist.desktop.playlist.PlaylistSongMetadataStore
import com.metrolist.desktop.sync.DesktopPlaylistSyncTarget
import com.metrolist.desktop.sync.PlaylistSyncCoordinator
import com.metrolist.desktop.sync.LikedSongsSync
import com.metrolist.desktop.sync.PlaylistCollaborationAccess
import com.metrolist.desktop.sync.PlaylistCollaborationRepository
import com.metrolist.desktop.sync.PlaylistInvitation
import com.metrolist.desktop.sync.PlaylistInviteLink
import com.metrolist.desktop.sync.PlaylistSyncStore
import com.metrolist.desktop.sync.RemotePlaylistApplicator
import com.metrolist.desktop.sync.SupabasePlaylistSyncRemote
import com.metrolist.desktop.sync.SyncOperationFactory
import com.metrolist.innertube.models.SongItem
import java.awt.EventQueue
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class HikalistController(
    val repository: DataRepository = DataRepository(),
    val lyricsResolver: LyricsResolver = LyricsResolver.default(),
    val offlineMediaStore: OfflineMediaStore = OfflineMediaStore(),
    val downloadManager: OfflineDownloadManager = OfflineDownloadManager.default(offlineMediaStore, lyricsResolver),
    val player: DesktopMusicPlayer = DesktopMusicPlayer(offlineMediaProvider = offlineMediaStore::find),
    private val metadataStore: PlaylistMetadataStore = PlaylistMetadataStore(),
    private val songMetadataStore: PlaylistSongMetadataStore = PlaylistSongMetadataStore(),
    private val syncStore: PlaylistSyncStore = PlaylistSyncStore(),
    private val syncOperationFactory: SyncOperationFactory = SyncOperationFactory(),
    private val collaborationRepository: PlaylistCollaborationRepository = PlaylistCollaborationRepository(),
    val authManager: DesktopAuthManager = DesktopAuthManager(),
    syncRemote: SupabasePlaylistSyncRemote = SupabasePlaylistSyncRemote(),
) {
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncApplicator = RemotePlaylistApplicator(
        DesktopPlaylistSyncTarget(repository, metadataStore, songMetadataStore) {
            EventQueue.invokeLater(::refreshLibrary)
        },
    )
    private val syncCoordinator = PlaylistSyncCoordinator(
        store = syncStore,
        remote = syncRemote,
        applyRemote = syncApplicator::apply,
    )
    val accountState = authManager.state
    val syncStatus = syncCoordinator.status
    val downloadStates = downloadManager.states
    var playlists by mutableStateOf<List<PlaylistRow>>(emptyList())
        private set
    var likedSongs by mutableStateOf<List<SongRow>>(emptyList())
        private set
    var likedSongMetadata by mutableStateOf<Map<String, PlaylistSongMetadata>>(emptyMap())
        private set
    var playlistThumbnails by mutableStateOf<Map<String, List<String?>>>(emptyMap())
        private set
    var playlistDescriptions by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var selectedPlaylist by mutableStateOf<PlaylistRow?>(null)
        private set
    var selectedPlaylistSongs by mutableStateOf<List<SongRow>>(emptyList())
        private set
    var selectedPlaylistSongMetadata by mutableStateOf<Map<String, PlaylistSongMetadata>>(emptyMap())
        private set
    var playlistAccess by mutableStateOf<Map<String, PlaylistCollaborationAccess>>(emptyMap())
        private set
    var collaborationBusy by mutableStateOf(false)
        private set
    var collaborationMessage by mutableStateOf<String?>(null)
        private set
    var lastInvitation by mutableStateOf<PlaylistInvitation?>(null)
        private set
    var lastInviteLink by mutableStateOf<PlaylistInviteLink?>(null)
        private set

    val selectedPlaylistAccess: PlaylistCollaborationAccess?
        get() = selectedPlaylist?.id?.let(playlistAccess::get)

    init {
        refreshLibrary()
        syncCoordinator.start()
        controllerScope.launch {
            accountState.map { it.status to it.userId }.distinctUntilChanged().collect { (status, userId) ->
                if (status == DesktopAccountStatus.SIGNED_IN && userId != null) {
                    enqueueInitialPlaylistSnapshot(userId)
                    syncCoordinator.requestSync()
                    refreshCollaborationAccess()
                }
            }
        }
    }

    fun refreshLibrary() {
        playlists = repository.getAllPlaylists()
        likedSongs = repository.getLikedSongs()
        likedSongMetadata = songMetadataStore.getForPlaylist(LIKED_SONGS_PLAYLIST_ID)
        playlistThumbnails = playlists.associate { playlist ->
            playlist.id to repository.getPlaylistSongs(playlist.id).map(SongRow::thumbnailUrl)
        }
        playlistDescriptions = playlists.mapNotNull { playlist ->
            metadataStore.getDescription(playlist.id)?.let { playlist.id to it }
        }.toMap()
        selectedPlaylist?.id?.let(::openPlaylist)
    }

    fun openPlaylist(id: String) {
        selectedPlaylist = repository.getPlaylistById(id)
        selectedPlaylistSongs = repository.getPlaylistSongs(id)
        selectedPlaylistSongMetadata = songMetadataStore.getForPlaylist(id)
        refreshCollaborationAccess(id)
    }

    fun closePlaylist() {
        selectedPlaylist = null
        selectedPlaylistSongs = emptyList()
        selectedPlaylistSongMetadata = emptyMap()
    }

    fun createPlaylist(name: String): PlaylistRow {
        val playlist = repository.createPlaylist(name.trim())
        refreshLibrary()
        openPlaylist(playlist.id)
        enqueuePlaylist(playlist.id)
        return playlist
    }

    fun importSpotifyPlaylist(name: String, description: String, songs: List<SongItem>): PlaylistRow {
        require(name.isNotBlank()) { "Enter a playlist name" }
        val uniqueSongs = songs.distinctBy(SongItem::id)
        require(uniqueSongs.isNotEmpty()) { "No matched songs to import" }

        val playlist = repository.createPlaylist(name.trim())
        metadataStore.setDescription(playlist.id, description.trim().take(300))
        repository.addSongsToPlaylist(playlist.id, uniqueSongs.map { it.toQueueItem().toSongRow() })
        val addedBy = accountState.value.username ?: PlaylistSongMetadataStore.LOCAL_CONTRIBUTOR
        val addedAt = System.currentTimeMillis() / 1_000L
        uniqueSongs.forEach { song ->
            songMetadataStore.recordIfAbsent(playlist.id, song.id, addedBy, addedAt)
        }
        refreshLibrary()
        openPlaylist(playlist.id)
        enqueuePlaylist(playlist.id)
        uniqueSongs.forEach { enqueueSong(playlist.id, it.id) }
        return playlist
    }

    fun renameSelectedPlaylist(name: String) {
        val playlist = selectedPlaylist ?: return
        updatePlaylistDetails(playlist.id, name, playlistDescriptions[playlist.id].orEmpty())
    }

    fun updateSelectedPlaylistCover(coverPath: String?) {
        val playlist = selectedPlaylist ?: return
        updatePlaylistCover(playlist.id, coverPath)
    }

    fun updatePlaylistDetails(playlistId: String, name: String, description: String) {
        if (!canManagePlaylist(playlistId)) return
        if (name.isBlank()) return
        repository.renamePlaylist(playlistId, name)
        metadataStore.setDescription(playlistId, description)
        refreshLibrary()
        enqueuePlaylist(playlistId)
    }

    fun updatePlaylistCover(playlistId: String, coverPath: String?) {
        if (!canManagePlaylist(playlistId)) return
        repository.updatePlaylistCover(playlistId, coverPath)
        refreshLibrary()
        enqueuePlaylist(playlistId)
    }

    fun deleteSelectedPlaylist() {
        val playlist = selectedPlaylist ?: return
        deletePlaylist(playlist.id)
    }

    fun deletePlaylist(playlistId: String) {
        if (!canManagePlaylist(playlistId)) return
        syncCoordinator.enqueue(syncOperationFactory.playlistDelete(playlistId))
        repository.deletePlaylist(playlistId)
        metadataStore.delete(playlistId)
        songMetadataStore.deletePlaylist(playlistId)
        if (selectedPlaylist?.id == playlistId) closePlaylist()
        refreshLibrary()
    }

    fun addToPlaylist(playlistId: String, item: DesktopMusicPlayer.QueueItem) {
        if (!canEditPlaylistSongs(playlistId)) return
        repository.saveSong(item.toSongRow())
        repository.addSongToPlaylist(playlistId, item.videoId)
        songMetadataStore.recordIfAbsent(
            playlistId = playlistId,
            songId = item.videoId,
            addedBy = accountState.value.username ?: item.addedBy ?: PlaylistSongMetadataStore.LOCAL_CONTRIBUTOR,
            addedAtEpochSeconds = item.addedAt ?: System.currentTimeMillis() / 1_000L,
        )
        refreshLibrary()
        enqueueSong(playlistId, item.videoId)
    }

    fun removeFromSelectedPlaylist(songId: String) {
        val playlist = selectedPlaylist ?: return
        if (!canEditPlaylistSongs(playlist.id)) return
        repository.removeSongFromPlaylist(playlist.id, songId)
        songMetadataStore.remove(playlist.id, songId)
        syncCoordinator.enqueue(syncOperationFactory.songDelete(playlist.id, songId))
        openPlaylist(playlist.id)
    }

    fun moveSelectedPlaylistSong(fromIndex: Int, toIndex: Int) {
        val playlist = selectedPlaylist ?: return
        if (!canEditPlaylistSongs(playlist.id)) return
        repository.movePlaylistSong(playlist.id, fromIndex, toIndex)
        openPlaylist(playlist.id)
        selectedPlaylistSongs.forEach { song -> enqueueSong(playlist.id, song.id) }
    }

    fun toggleLike(item: DesktopMusicPlayer.QueueItem) {
        repository.saveSong(item.toSongRow())
        val shouldLike = likedSongs.none { it.id == item.videoId }
        repository.setSongLiked(item.videoId, shouldLike)
        if (shouldLike) {
            songMetadataStore.recordIfAbsent(
                playlistId = LikedSongsSync.LOCAL_PLAYLIST_ID,
                songId = item.videoId,
                addedBy = accountState.value.username ?: item.addedBy ?: PlaylistSongMetadataStore.LOCAL_CONTRIBUTOR,
                addedAtEpochSeconds = item.addedAt ?: System.currentTimeMillis() / 1_000L,
            )
        } else {
            songMetadataStore.remove(LikedSongsSync.LOCAL_PLAYLIST_ID, item.videoId)
        }
        refreshLibrary()
        syncCoordinator.enqueue(syncOperationFactory.likedPlaylistUpsert())
        if (shouldLike) enqueueLikedSong(item.videoId)
        else syncCoordinator.enqueue(syncOperationFactory.songDelete(LikedSongsSync.LOCAL_PLAYLIST_ID, item.videoId))
    }

    fun isLiked(videoId: String): Boolean = likedSongs.any { it.id == videoId }

    fun download(item: DesktopMusicPlayer.QueueItem) {
        downloadManager.enqueue(item.toOfflineDownloadRequest())
    }

    fun downloadPlaylist(items: List<DesktopMusicPlayer.QueueItem>) {
        downloadManager.enqueue(items.map { it.toOfflineDownloadRequest() })
    }

    fun cancelDownload(videoId: String) = downloadManager.cancel(videoId)

    fun removeDownload(videoId: String) = downloadManager.remove(videoId)

    fun removePlaylistDownloads(videoIds: List<String>) {
        videoIds.distinct().forEach(downloadManager::remove)
    }

    fun isDownloaded(videoId: String): Boolean = downloadManager.isDownloaded(videoId)

    fun recordSearch(query: String) {
        repository.addSearchQuery(query)
    }

    fun requestPlaylistSync() = syncCoordinator.requestSync()

    fun openLogin() = authManager.begin(DesktopAuthMode.LOGIN)

    fun openRegistration() = authManager.begin(DesktopAuthMode.REGISTER)

    fun signOut() = authManager.signOut()

    fun updateProfile(username: String, avatarFile: File?) = authManager.updateProfile(username, avatarFile)

    fun canManagePlaylist(playlistId: String): Boolean =
        repository.getPlaylistById(playlistId)?.isLocal == true || playlistAccess[playlistId]?.canManagePlaylist == true

    fun canEditPlaylistSongs(playlistId: String): Boolean =
        repository.getPlaylistById(playlistId)?.isLocal == true || playlistAccess[playlistId]?.canEditSongs == true

    fun inviteEditor(username: String) {
        val playlistId = selectedPlaylist?.id ?: return
        val currentUserId = accountState.value.userId ?: run {
            collaborationMessage = "Sign in before inviting an editor"
            return
        }
        controllerScope.launch {
            publishCollaborationBusy(true)
            runCatching {
                syncCoordinator.runOnce()
                collaborationRepository.addEditor(playlistId, username, currentUserId)
            }.onSuccess { invitation ->
                EventQueue.invokeLater {
                    lastInvitation = invitation
                    collaborationMessage = "${invitation.collaborator.username} can now add and remove songs"
                }
                refreshCollaborationAccessNow(playlistId, currentUserId)
                syncCoordinator.requestSync()
            }.onFailure { error ->
                EventQueue.invokeLater {
                    collaborationMessage = error.message ?: "Could not invite this editor"
                }
            }
            publishCollaborationBusy(false)
        }
    }

    fun removeEditor(userId: String) {
        val playlistId = selectedPlaylist?.id ?: return
        val currentUserId = accountState.value.userId ?: return
        controllerScope.launch {
            publishCollaborationBusy(true)
            runCatching { collaborationRepository.removeEditor(playlistId, userId, currentUserId) }
                .onSuccess {
                    EventQueue.invokeLater {
                        collaborationMessage = "Editor removed"
                        lastInvitation = null
                    }
                    refreshCollaborationAccessNow(playlistId, currentUserId)
                    syncCoordinator.requestSync()
                }
                .onFailure { error ->
                    EventQueue.invokeLater {
                        collaborationMessage = error.message ?: "Could not remove editor"
                    }
                }
            publishCollaborationBusy(false)
        }
    }

    fun createEditorInviteLink() {
        val playlistId = selectedPlaylist?.id ?: return
        val currentUserId = accountState.value.userId ?: run {
            collaborationMessage = "Sign in before creating an invite link"
            return
        }
        controllerScope.launch {
            publishCollaborationBusy(true)
            runCatching {
                syncCoordinator.runOnce()
                collaborationRepository.createEditorInviteLink(playlistId, currentUserId)
            }.onSuccess { invite ->
                EventQueue.invokeLater {
                    lastInviteLink = invite
                    collaborationMessage = "Invite link is active for 2 hours"
                }
            }.onFailure { error ->
                EventQueue.invokeLater {
                    collaborationMessage = error.message ?: "Could not create an invite link"
                }
            }
            publishCollaborationBusy(false)
        }
    }

    fun revokeEditorInviteLink() {
        val playlistId = selectedPlaylist?.id ?: return
        val currentUserId = accountState.value.userId ?: return
        controllerScope.launch {
            publishCollaborationBusy(true)
            runCatching { collaborationRepository.revokeEditorInviteLinks(playlistId, currentUserId) }
                .onSuccess {
                    EventQueue.invokeLater {
                        lastInviteLink = null
                        collaborationMessage = "Invite link disabled"
                    }
                }.onFailure { error ->
                    EventQueue.invokeLater {
                        collaborationMessage = error.message ?: "Could not disable the invite link"
                    }
                }
            publishCollaborationBusy(false)
        }
    }

    fun clearCollaborationMessage() {
        collaborationMessage = null
        lastInvitation = null
        lastInviteLink = null
    }

    fun homePersonalizationSeeds(): List<HomeSeed> {
        val recentPlayed = repository.getRecentPlayedSongs()
        val librarySongs = repository.getAllSongs()
        return HomePersonalization.seeds(
            PersonalizationSignals(
                recentSearches = repository.getSearchHistory(),
                likedArtists = likedSongs.flatMap { it.artists.orEmpty() },
                recentlyPlayedArtists = recentPlayed.flatMap { it.artists.orEmpty() },
                libraryArtists = librarySongs.flatMap { it.artists.orEmpty() },
            ),
        )
    }

    fun play(items: List<DesktopMusicPlayer.QueueItem>, selectedIndex: Int) {
        items.getOrNull(selectedIndex)?.let { selected ->
            repository.saveSong(selected.toSongRow())
            repository.recordPlay(selected.videoId, playTimeMs = 0L)
        }
        player.setQueue(items, selectedIndex)
    }

    fun dispose() {
        player.dispose()
        downloadManager.close()
        controllerScope.cancel()
        authManager.close()
        runBlocking(Dispatchers.IO) { syncCoordinator.close() }
    }

    private fun enqueueInitialPlaylistSnapshot(userId: String) {
        val stateKey = "$BOOTSTRAP_STATE_KEY.$userId"
        if (syncStore.loadState(stateKey) == BOOTSTRAP_COMPLETE) return
        playlists.forEach { playlist ->
            enqueuePlaylist(playlist.id)
            repository.getPlaylistSongs(playlist.id).forEach { song -> enqueueSong(playlist.id, song.id) }
        }
        syncCoordinator.enqueue(syncOperationFactory.likedPlaylistUpsert())
        likedSongs.forEach { song -> enqueueLikedSong(song.id) }
        syncStore.saveState(stateKey, BOOTSTRAP_COMPLETE)
    }

    private fun enqueuePlaylist(playlistId: String) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        syncCoordinator.enqueue(
            syncOperationFactory.playlistUpsert(
                playlist = playlist,
                description = metadataStore.getDescription(playlistId).orEmpty(),
            ),
        )
    }

    private fun enqueueSong(playlistId: String, songId: String) {
        val songs = repository.getPlaylistSongs(playlistId)
        val position = songs.indexOfFirst { it.id == songId }
        val song = songs.getOrNull(position) ?: return
        val metadata = songMetadataStore.getForPlaylist(playlistId)[songId]
        syncCoordinator.enqueue(syncOperationFactory.songUpsert(playlistId, song, position, metadata))
    }

    private fun enqueueLikedSong(songId: String) {
        val position = likedSongs.indexOfFirst { it.id == songId }
        val song = likedSongs.getOrNull(position) ?: return
        val metadata = songMetadataStore.getForPlaylist(LikedSongsSync.LOCAL_PLAYLIST_ID)[songId]
        syncCoordinator.enqueue(
            syncOperationFactory.songUpsert(LikedSongsSync.LOCAL_PLAYLIST_ID, song, position, metadata),
        )
    }

    private fun refreshCollaborationAccess(playlistId: String? = null) {
        val currentUserId = accountState.value.userId ?: return
        val ids = playlistId?.let(::listOf) ?: playlists.map(PlaylistRow::id)
        controllerScope.launch {
            ids.forEach { id -> runCatching { refreshCollaborationAccessNow(id, currentUserId) } }
        }
    }

    private suspend fun refreshCollaborationAccessNow(playlistId: String, currentUserId: String) {
        val access = collaborationRepository.access(playlistId, currentUserId)
        EventQueue.invokeLater { playlistAccess = playlistAccess + (playlistId to access) }
    }

    private fun publishCollaborationBusy(value: Boolean) {
        EventQueue.invokeLater { collaborationBusy = value }
    }

    private companion object {
        const val LIKED_SONGS_PLAYLIST_ID = LikedSongsSync.LOCAL_PLAYLIST_ID
        const val BOOTSTRAP_STATE_KEY = "playlist_sync_bootstrap_v1"
        const val BOOTSTRAP_COMPLETE = "complete"
    }
}

fun SongItem.toQueueItem(playlistId: String? = null) = DesktopMusicPlayer.QueueItem(
    videoId = id,
    playlistId = playlistId,
    title = title,
    artist = artists.joinToString(", ") { it.name },
    album = album?.name,
    thumbnailUrl = thumbnail.takeIf(String::isNotBlank),
    duration = duration ?: 0,
)

fun SongRow.toQueueItem(playlistId: String? = null) = DesktopMusicPlayer.QueueItem(
    videoId = id,
    playlistId = playlistId,
    title = title,
    artist = artists.orEmpty().joinToString(", ").ifBlank { "Unknown artist" },
    album = albumName ?: album,
    thumbnailUrl = thumbnailUrl,
    duration = duration.coerceAtLeast(0),
)

fun DesktopMusicPlayer.QueueItem.toSongRow() = SongRow(
    id = videoId,
    title = title,
    duration = duration,
    thumbnailUrl = thumbnailUrl,
    albumName = album,
    artists = artist.split(',').map(String::trim).filter(String::isNotBlank),
    album = album,
)
