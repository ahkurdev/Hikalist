package com.metrolist.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.desktop.db.repository.PlaylistRow
import com.metrolist.desktop.db.repository.SongRow
import com.metrolist.desktop.discord.DiscordPresenceManager
import com.metrolist.desktop.lyrics.LyricLine
import com.metrolist.desktop.lyrics.LyricsRequest
import com.metrolist.desktop.lyrics.LyricsResolver
import com.metrolist.desktop.lyrics.SyncedLyricsParser
import com.metrolist.desktop.offline.OfflineDownloadManager
import com.metrolist.desktop.offline.OfflineDownloadState
import com.metrolist.desktop.offline.OfflineMediaStore
import com.metrolist.desktop.personalization.HomePersonalization
import com.metrolist.desktop.personalization.HomeSeed
import com.metrolist.desktop.preferences.AppSettings
import com.metrolist.desktop.preferences.RepeatPreference
import com.metrolist.desktop.player.DesktopMusicPlayer
import com.metrolist.desktop.player.EqualizerBand
import com.metrolist.desktop.player.EqualizerConfiguration
import com.metrolist.desktop.player.PlaybackNotice
import com.metrolist.desktop.player.RepeatMode
import com.metrolist.desktop.player.QueueSessionStore
import com.metrolist.desktop.player.SleepTimerController
import com.metrolist.desktop.storage.StorageManager
import com.metrolist.desktop.update.UpdateChecker
import com.metrolist.desktop.playlist.PlaylistCoverStore
import com.metrolist.desktop.playlist.PlaylistSongMetadata
import com.metrolist.desktop.playlist.PlaylistSongMetadataFormatter
import com.metrolist.desktop.ui.components.HikalistLogo
import com.metrolist.desktop.ui.components.PlaylistArtwork
import com.metrolist.desktop.ui.screens.SearchScreen
import com.metrolist.desktop.ui.screens.SettingsScreen
import com.metrolist.desktop.ui.screens.EqualizerScreen
import com.metrolist.desktop.windows.SystemMediaSessionManager
import com.metrolist.desktop.ui.screens.ContentDetailScreen
import com.metrolist.desktop.sync.PlaylistCollaborationAccess
import com.metrolist.desktop.sync.PlaylistInviteLink
import com.metrolist.desktop.sync.PlaylistMemberRole
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.HomePage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class MainDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    SEARCH("Search", Icons.Outlined.Search, Icons.Filled.Search),
    LIBRARY("Library", Icons.Outlined.LibraryMusic, Icons.Filled.LibraryMusic),
    SETTINGS("Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

private fun RepeatPreference.toPlayerRepeatMode(): RepeatMode = when (this) {
    RepeatPreference.OFF -> RepeatMode.OFF
    RepeatPreference.ALL -> RepeatMode.ALL
    RepeatPreference.ONE -> RepeatMode.ONE
}

private fun RepeatMode.toPreference(): RepeatPreference = when (this) {
    RepeatMode.OFF -> RepeatPreference.OFF
    RepeatMode.ALL -> RepeatPreference.ALL
    RepeatMode.ONE -> RepeatPreference.ONE
}

@Composable
fun AppLayout(settings: AppSettings) {
    val controller = remember(settings) {
        val offlineMediaStore = OfflineMediaStore()
        val lyricsResolver = LyricsResolver.default()
        HikalistController(
            lyricsResolver = lyricsResolver,
            offlineMediaStore = offlineMediaStore,
            downloadManager = OfflineDownloadManager.default(
                store = offlineMediaStore,
                lyricsResolver = lyricsResolver,
                audioQualityProvider = { settings.audioQuality.value },
            ),
            player = DesktopMusicPlayer(
                initialVolumePercent = settings.volumePercent.value,
                initialRepeatMode = settings.repeat.value.toPlayerRepeatMode(),
                audioQualityProvider = { settings.audioQuality.value },
                offlineMediaProvider = offlineMediaStore::find,
                queueSessionStore = QueueSessionStore(),
                initialAudioOutputId = settings.audioOutputId.value,
            ),
        )
    }
    val preferredRepeat by settings.repeat.collectAsState()
    val equalizerEnabled by settings.equalizerEnabled.collectAsState()
    val equalizerPreset by settings.equalizerPreset.collectAsState()
    val equalizerGains by settings.equalizerGainsDb.collectAsState()
    val playbackNotice by controller.player.playbackNotice.collectAsState()
    val downloadStates by controller.downloadStates.collectAsState()
    val coverStore = remember { PlaylistCoverStore() }
    val scope = rememberCoroutineScope()
    var destination by remember { mutableStateOf(MainDestination.HOME) }
    var equalizerVisible by remember { mutableStateOf(false) }
    var fullPlayerVisible by remember { mutableStateOf(false) }
    var likedPlaylistOpen by remember { mutableStateOf(false) }
    var queueVisible by remember { mutableStateOf(false) }
    var createPlaylistVisible by remember { mutableStateOf(false) }
    var addToPlaylistItem by remember { mutableStateOf<DesktopMusicPlayer.QueueItem?>(null) }
    var coverError by remember { mutableStateOf<String?>(null) }
    var editingPlaylist by remember { mutableStateOf<PlaylistRow?>(null) }
    var deletingPlaylist by remember { mutableStateOf<PlaylistRow?>(null) }
    var collaborationPlaylist by remember { mutableStateOf<PlaylistRow?>(null) }
    var contentHistory by remember { mutableStateOf(ContentHistory()) }
    val showBottomPlayerBar = PlayerUiGeometry.showBottomPlayerBar(fullPlayerVisible)
    val discordPresenceManager = remember(controller, settings) {
        DiscordPresenceManager(
            player = controller.player,
            enabled = settings.discordRichPresence,
        )
    }
    val systemMediaSessionManager = remember(controller) { SystemMediaSessionManager(controller.player) }
    val storageManager = remember { StorageManager() }
    val sleepTimerController = remember(controller) {
        SleepTimerController(
            stopPlayback = controller.player::stop,
            setStopAfterCurrent = controller.player::setStopAfterCurrent,
        )
    }
    val updateChecker = remember { UpdateChecker() }

    DisposableEffect(controller, discordPresenceManager, systemMediaSessionManager, sleepTimerController) {
        discordPresenceManager.start()
        systemMediaSessionManager.start()
        onDispose {
            systemMediaSessionManager.close()
            discordPresenceManager.close()
            sleepTimerController.close()
            controller.dispose()
        }
    }
    LaunchedEffect(preferredRepeat) {
        controller.player.setRepeatMode(preferredRepeat.toPlayerRepeatMode())
    }
    LaunchedEffect(equalizerEnabled, equalizerGains) {
        controller.player.setEqualizer(
            EqualizerConfiguration(
                enabled = equalizerEnabled,
                gainsDb = EqualizerBand.entries.zip(equalizerGains).toMap(),
            ),
        )
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            if (fullPlayerVisible) {
                SongScreen(
                    controller = controller,
                    settings = settings,
                    queueVisible = queueVisible,
                    onToggleQueue = { queueVisible = !queueVisible },
                    onClose = { fullPlayerVisible = false },
                    onAddToPlaylist = { addToPlaylistItem = it },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Row(Modifier.fillMaxSize().padding(bottom = 108.dp)) {
                    Sidebar(
                        destination = destination,
                        playlists = controller.playlists,
                        playlistThumbnails = controller.playlistThumbnails,
                        likedThumbnails = controller.likedSongs.map(SongRow::thumbnailUrl),
                        onSelectDestination = {
                            controller.closePlaylist()
                            likedPlaylistOpen = false
                            contentHistory = contentHistory.clear()
                            destination = it
                        },
                        onOpenLiked = {
                            controller.closePlaylist()
                            likedPlaylistOpen = true
                            contentHistory = contentHistory.clear()
                            destination = MainDestination.LIBRARY
                        },
                        onOpenPlaylist = { id ->
                            likedPlaylistOpen = false
                            contentHistory = contentHistory.clear()
                            controller.openPlaylist(id)
                            destination = MainDestination.LIBRARY
                        },
                        onNewPlaylist = { createPlaylistVisible = true },
                        onEditPlaylist = { editingPlaylist = it },
                        onDeletePlaylist = { deletingPlaylist = it },
                        canManagePlaylist = controller::canManagePlaylist,
                    )
                    Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.background)) {
                        val selectedPlaylist = controller.selectedPlaylist
                        val contentDestination = contentHistory.current
                        if (likedPlaylistOpen) {
                            PlaylistDetailScreen(
                                playlist = PlaylistRow(id = "LIKED_SONGS", name = "Liked Songs", isLocal = true),
                                songs = controller.likedSongs,
                                songMetadata = controller.likedSongMetadata,
                                eyebrow = "AUTOMATIC PLAYLIST",
                                description = "Every song you love, kept in one place.",
                                onBack = { likedPlaylistOpen = false },
                                onPlay = { index -> controller.play(controller.likedSongs.map(SongRow::toQueueItem), index) },
                                onShuffle = {
                                    val items = controller.likedSongs.map(SongRow::toQueueItem)
                                    if (items.isNotEmpty()) {
                                        controller.play(items, 0)
                                        controller.player.setShuffle(true)
                                    }
                                },
                                onRemove = { songId ->
                                    controller.likedSongs.firstOrNull { it.id == songId }?.let { controller.toggleLike(it.toQueueItem()) }
                                },
                                onMove = { _, _ -> },
                                onEdit = {},
                                onDelete = {},
                                onCollaborate = {},
                                canManage = false,
                                canEditSongs = false,
                                downloadStates = downloadStates,
                                isDownloaded = controller::isDownloaded,
                                onDownloadSong = { controller.download(it.toQueueItem()) },
                                onCancelDownload = controller::cancelDownload,
                                onRemoveDownload = controller::removeDownload,
                                onDownloadAll = { controller.downloadPlaylist(controller.likedSongs.map { it.toQueueItem() }) },
                                onRemoveAllDownloads = {
                                    controller.removePlaylistDownloads(controller.likedSongs.map(SongRow::id))
                                },
                            )
                        } else if (selectedPlaylist != null) {
                            PlaylistDetailScreen(
                                playlist = selectedPlaylist,
                                songs = controller.selectedPlaylistSongs,
                                songMetadata = controller.selectedPlaylistSongMetadata,
                                onBack = controller::closePlaylist,
                                onPlay = { index ->
                                    controller.play(controller.selectedPlaylistSongs.map { it.toQueueItem(selectedPlaylist.id) }, index)
                                },
                                onShuffle = {
                                    val items = controller.selectedPlaylistSongs.map { it.toQueueItem(selectedPlaylist.id) }
                                    if (items.isNotEmpty()) {
                                        controller.play(items, 0)
                                        controller.player.setShuffle(true)
                                    }
                                },
                                onRemove = controller::removeFromSelectedPlaylist,
                                onMove = controller::moveSelectedPlaylistSong,
                                description = controller.playlistDescriptions[selectedPlaylist.id]
                                    ?: "A personal collection in Hikalist",
                                onEdit = { editingPlaylist = selectedPlaylist },
                                onDelete = { deletingPlaylist = selectedPlaylist },
                                onCollaborate = { collaborationPlaylist = selectedPlaylist },
                                canManage = controller.canManagePlaylist(selectedPlaylist.id),
                                canEditSongs = controller.canEditPlaylistSongs(selectedPlaylist.id),
                                downloadStates = downloadStates,
                                isDownloaded = controller::isDownloaded,
                                onDownloadSong = { controller.download(it.toQueueItem(selectedPlaylist.id)) },
                                onCancelDownload = controller::cancelDownload,
                                onRemoveDownload = controller::removeDownload,
                                onDownloadAll = {
                                    controller.downloadPlaylist(
                                        controller.selectedPlaylistSongs.map { it.toQueueItem(selectedPlaylist.id) },
                                    )
                                },
                                onRemoveAllDownloads = {
                                    controller.removePlaylistDownloads(controller.selectedPlaylistSongs.map(SongRow::id))
                                },
                            )
                        } else {
                            when (destination) {
                                MainDestination.HOME -> HomeScreen(
                                    loadPersonalSeeds = controller::homePersonalizationSeeds,
                                    onPlay = controller::play,
                                    onAddToPlaylist = { addToPlaylistItem = it },
                                    onOpenContent = { item ->
                                        contentDestinationFor(item)?.let { contentHistory = contentHistory.open(it) }
                                    },
                                )
                                MainDestination.SEARCH -> SearchScreen(
                                    onPlay = controller::play,
                                    onAddToPlaylist = { addToPlaylistItem = it },
                                    onSearchRecorded = controller::recordSearch,
                                    onOpenContent = { item ->
                                        contentDestinationFor(item)?.let { contentHistory = contentHistory.open(it) }
                                    },
                                )
                                MainDestination.LIBRARY -> LibraryScreen(
                                    playlists = controller.playlists,
                                    playlistThumbnails = controller.playlistThumbnails,
                                    likedSongs = controller.likedSongs,
                                    onOpenPlaylist = controller::openPlaylist,
                                    onOpenLiked = { likedPlaylistOpen = true },
                                    onNewPlaylist = { createPlaylistVisible = true },
                                    playlistDescriptions = controller.playlistDescriptions,
                                )
                                MainDestination.SETTINGS -> if (equalizerVisible) {
                                    EqualizerScreen(
                                        settings = settings,
                                        onBack = { equalizerVisible = false },
                                    )
                                } else SettingsScreen(
                                    settings = settings,
                                    accountState = controller.accountState,
                                    syncStatus = controller.syncStatus,
                                    onLogin = controller::openLogin,
                                    onRegister = controller::openRegistration,
                                    onSignOut = controller::signOut,
                                    onUpdateProfile = controller::updateProfile,
                                    onSyncNow = controller::requestPlaylistSync,
                                    onVolumePreview = controller.player::setVolume,
                                    onOpenEqualizer = { equalizerVisible = true },
                                    audioOutputDevices = controller.player.availableAudioOutputs(),
                                    onAudioOutputSelected = { deviceId ->
                                        settings.setAudioOutputId(deviceId)
                                        controller.player.setAudioOutput(deviceId)
                                    },
                                    storageManager = storageManager,
                                    sleepTimerState = sleepTimerController.state,
                                    onSleepTimerSelected = sleepTimerController::select,
                                    updateChecker = updateChecker,
                                )
                            }
                        }
                        contentDestination?.let {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                ContentDetailScreen(
                                    destination = it,
                                    onBack = { contentHistory = contentHistory.back() },
                                    onOpenContent = { item ->
                                        contentDestinationFor(item)?.let { next -> contentHistory = contentHistory.open(next) }
                                    },
                                    onPlay = controller::play,
                                    onShuffle = { items ->
                                        controller.play(items, 0)
                                        controller.player.setShuffle(true)
                                    },
                                    onAddToPlaylist = { addToPlaylistItem = it },
                                )
                            }
                        }
                    }
                }
            }

            if (showBottomPlayerBar) {
                PlayerBar(
                    controller = controller,
                    settings = settings,
                    onOpenSong = { fullPlayerVisible = true },
                    onOpenLyrics = { fullPlayerVisible = true; queueVisible = false },
                    onAddToPlaylist = { addToPlaylistItem = it },
                    onToggleQueue = { queueVisible = !queueVisible; fullPlayerVisible = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }

            playbackNotice?.let { notice ->
                PlaybackNoticeBanner(
                    notice = notice,
                    onRetry = controller.player::retryPlayback,
                    onDismiss = controller.player::dismissPlaybackNotice,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (showBottomPlayerBar) 116.dp else 12.dp),
                )
            }
        }
    }

    if (createPlaylistVisible) {
        PlaylistCreationDialog(
            onDismiss = { createPlaylistVisible = false },
            onCreateEmpty = { controller.createPlaylist(it); createPlaylistVisible = false },
            onImportSpotify = { name, description, songs ->
                controller.importSpotifyPlaylist(name, description, songs)
                createPlaylistVisible = false
            },
        )
    }
    addToPlaylistItem?.let { item ->
        AddToPlaylistDialog(
            item = item,
            playlists = controller.playlists,
            playlistAccess = controller.playlistAccess,
            onDismiss = { addToPlaylistItem = null },
            onCreatePlaylist = { controller.createPlaylist(it) },
            onAdd = { playlistId -> controller.addToPlaylist(playlistId, item); addToPlaylistItem = null },
        )
    }
    editingPlaylist?.let { playlist ->
        EditPlaylistDialog(
            playlist = playlist,
            description = controller.playlistDescriptions[playlist.id].orEmpty(),
            songThumbnails = controller.playlistThumbnails[playlist.id].orEmpty(),
            onDismiss = { editingPlaylist = null },
            onSave = { name, description, replacementCover, resetCover ->
                controller.updatePlaylistDetails(playlist.id, name, description)
                val previousCover = playlist.thumbnailUrl
                when {
                    replacementCover != null -> scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) { coverStore.save(playlist.id, replacementCover) }
                        }.onSuccess { saved ->
                            controller.updatePlaylistCover(playlist.id, saved.absolutePath)
                            withContext(Dispatchers.IO) { coverStore.deleteIfManaged(previousCover) }
                        }.onFailure { error ->
                            coverError = error.message ?: "Could not save this image."
                        }
                    }
                    resetCover -> {
                        controller.updatePlaylistCover(playlist.id, null)
                        scope.launch(Dispatchers.IO) { coverStore.deleteIfManaged(previousCover) }
                    }
                }
                editingPlaylist = null
            },
        )
    }
    deletingPlaylist?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onDismiss = { deletingPlaylist = null },
            onConfirm = {
                val previousCover = playlist.thumbnailUrl
                controller.deletePlaylist(playlist.id)
                scope.launch(Dispatchers.IO) { coverStore.deleteIfManaged(previousCover) }
                deletingPlaylist = null
            },
        )
    }
    collaborationPlaylist?.let { playlist ->
        PlaylistCollaborationDialog(
            playlist = playlist,
            access = controller.playlistAccess[playlist.id],
            busy = controller.collaborationBusy,
            message = controller.collaborationMessage,
            inviteLink = controller.lastInviteLink,
            onDismiss = {
                controller.clearCollaborationMessage()
                collaborationPlaylist = null
            },
            onInvite = controller::inviteEditor,
            onCreateLink = controller::createEditorInviteLink,
            onRevokeLink = controller::revokeEditorInviteLink,
            onRemove = controller::removeEditor,
        )
    }
    coverError?.let { message ->
        AlertDialog(
            onDismissRequest = { coverError = null },
            title = { Text("Cover could not be changed") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { coverError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun PlaybackNoticeBanner(
    notice: PlaybackNotice,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retrying = notice is PlaybackNotice.Retrying
    val failure = when (notice) {
        is PlaybackNotice.Retrying -> notice.failure
        is PlaybackNotice.Failed -> notice.failure
    }
    val containerColor = if (retrying) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val contentColor = if (retrying) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier.widthIn(max = 680.dp).padding(horizontal = 20.dp),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (retrying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                Icon(Icons.Default.ErrorOutline, null, Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(failure.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                val detail = when (notice) {
                    is PlaybackNotice.Retrying -> "Retrying ${notice.attempt} of ${notice.maxAttempts}…"
                    is PlaybackNotice.Failed -> failure.message
                }
                Text(detail, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.82f))
            }
            if (notice is PlaybackNotice.Failed) {
                TextButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Try again")
                }
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Close, "Dismiss playback message", Modifier.size(19.dp))
            }
        }
    }
}

@Composable
private fun Sidebar(
    destination: MainDestination,
    playlists: List<PlaylistRow>,
    playlistThumbnails: Map<String, List<String?>>,
    likedThumbnails: List<String?>,
    onSelectDestination: (MainDestination) -> Unit,
    onOpenLiked: () -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onNewPlaylist: () -> Unit,
    onEditPlaylist: (PlaylistRow) -> Unit,
    onDeletePlaylist: (PlaylistRow) -> Unit,
    canManagePlaylist: (String) -> Boolean,
) {
    Column(
        Modifier.width(252.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 20.dp),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            HikalistLogo(42.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Hikalist", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text("listen softly", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(26.dp))
        MainDestination.entries.forEach { item ->
            val selected = destination == item
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelectDestination(item) }.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (selected) item.selectedIcon else item.icon, item.label, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(14.dp))
                Text(item.label, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
            }
            Spacer(Modifier.height(4.dp))
        }

        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("YOUR PLAYLISTS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            IconButton(onClick = onNewPlaylist, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Add, "New playlist", Modifier.size(18.dp)) }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(Modifier.weight(1f)) {
            item {
                SidebarPlaylistRow("Liked Songs", null, likedThumbnails, onClick = onOpenLiked)
            }
            items(playlists, key = PlaylistRow::id) { playlist ->
                val canManage = canManagePlaylist(playlist.id)
                SidebarPlaylistRow(
                    name = playlist.name,
                    cover = playlist.thumbnailUrl,
                    thumbnails = playlistThumbnails[playlist.id].orEmpty(),
                    onClick = { onOpenPlaylist(playlist.id) },
                    onEdit = if (canManage) ({ onEditPlaylist(playlist) }) else null,
                    onDelete = if (canManage) ({ onDeletePlaylist(playlist) }) else null,
                )
            }
        }
    }
}

@Composable
private fun SidebarPlaylistRow(
    name: String,
    cover: String?,
    thumbnails: List<String?>,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistArtwork(cover, thumbnails, 36.dp, 8.dp)
        Spacer(Modifier.width(10.dp))
        Text(name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
        if (onEdit != null && onDelete != null) {
            PlaylistOptionsButton(onEdit, onDelete, Modifier.size(30.dp))
        }
    }
}

@Composable
private fun HomeScreen(
    loadPersonalSeeds: () -> List<HomeSeed>,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    onOpenContent: (YTItem) -> Unit,
) {
    var sections by remember { mutableStateOf<List<HomePage.Section>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val seeds = withContext(Dispatchers.IO) { loadPersonalSeeds() }
        sections = runCatching {
            coroutineScope {
                val discovery = async {
                    YouTube.home().getOrNull()?.sections.orEmpty()
                        .mapNotNull(HomePage.Section::supportedItemsOnly)
                }
                val personal = seeds.map { seed ->
                    async { loadPersonalHomeSection(seed) }
                }.awaitAll().filterNotNull()
                HomePersonalization.blend(personal, discovery.await())
            }
        }.getOrDefault(emptyList())
        loading = false
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(30.dp)) {
        item {
            Text("Good evening", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text("Music for your moment", style = MaterialTheme.typography.headlineLarge)
            Text("A calm corner for every sound you love.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (loading) {
            item { Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        } else if (sections.isEmpty()) {
            item { EmptyState("Home could not be loaded", "Check your connection and open Home again.") }
        } else {
            sections.forEach { section ->
                item {
                    Column {
                        Text(section.title.replace("�", "").trim(), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        HomeCarousel(
                            section = section,
                            onPlay = onPlay,
                            onAddToPlaylist = onAddToPlaylist,
                            onOpenContent = onOpenContent,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HomeCarousel(
    section: HomePage.Section,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    onOpenContent: (YTItem) -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val songItems = remember(section.items) { section.items.filterIsInstance<SongItem>() }
    var hovered by remember(section.title) { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .pointerMoveFilter(
                onEnter = {
                    hovered = true
                    false
                },
                onExit = {
                    hovered = false
                    false
                },
            ),
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(section.items, key = { homeItemKey(it) }) { item ->
                HomeCard(
                    item = item,
                    onClick = {
                        if (item is SongItem) {
                            val index = songItems.indexOfFirst { it.id == item.id }
                            onPlay(songItems.map(SongItem::toQueueItem), index.coerceAtLeast(0))
                        } else {
                            onOpenContent(item)
                        }
                    },
                    onAdd = if (item is SongItem) ({ onAddToPlaylist(item.toQueueItem()) }) else null,
                )
            }
        }

        AnimatedVisibility(
            visible = hovered && listState.canScrollBackward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = PlayerUiGeometry.carouselArrowTopOffsetDp().dp)
                .padding(start = 6.dp),
        ) {
            CarouselArrowButton(
                icon = Icons.Default.ChevronLeft,
                description = "Previous cards",
                onClick = {
                    val target = CarouselNavigation.previousIndex(listState.firstVisibleItemIndex)
                    scope.launch { listState.animateScrollToItem(target) }
                },
            )
        }

        AnimatedVisibility(
            visible = hovered && listState.canScrollForward,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = PlayerUiGeometry.carouselArrowTopOffsetDp().dp)
                .padding(end = 6.dp),
        ) {
            CarouselArrowButton(
                icon = Icons.Default.ChevronRight,
                description = "Next cards",
                onClick = {
                    val target = CarouselNavigation.nextIndex(
                        firstVisibleIndex = listState.firstVisibleItemIndex,
                        itemCount = section.items.size,
                    )
                    scope.launch { listState.animateScrollToItem(target) }
                },
            )
        }
    }
}

@Composable
private fun CarouselArrowButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 10.dp,
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(46.dp),
        ) {
            Icon(icon, description, Modifier.size(27.dp))
        }
    }
}

private suspend fun loadPersonalHomeSection(seed: HomeSeed): HomePage.Section? {
    val items = YouTube.searchSummary(seed.query).getOrNull()
        ?.summaries
        .orEmpty()
        .flatMap { it.items }
        .filter(::isSupportedHomeItem)
        .distinctBy(::homeItemKey)
        .take(12)
    if (items.isEmpty()) return null
    return HomePage.Section(
        title = seed.title,
        label = null,
        thumbnail = null,
        endpoint = null,
        items = items,
    )
}

private fun HomePage.Section.supportedItemsOnly(): HomePage.Section? {
    val supported = items.filter(::isSupportedHomeItem).distinctBy(::homeItemKey)
    return copy(items = supported).takeIf { supported.isNotEmpty() }
}

private fun isSupportedHomeItem(item: YTItem): Boolean =
    item is SongItem || item is AlbumItem || item is ArtistItem || item is PlaylistItem

private fun homeItemKey(item: YTItem): String = "${item::class.simpleName}-${item.id}"

@Composable
private fun HomeCard(item: YTItem, onClick: () -> Unit, onAdd: (() -> Unit)?) {
    val title = item.title
    val subtitle = when (item) {
        is SongItem -> item.artists.joinToString(", ") { it.name }
        is AlbumItem -> item.artists.orEmpty().joinToString(", ") { it.name }
        is ArtistItem -> "Artist"
        is PlaylistItem -> item.author?.name ?: "Playlist"
        else -> ""
    }
    val thumbnail = item.thumbnail
    Column(Modifier.width(176.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(bottom = 8.dp)) {
        Box(Modifier.size(176.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            if (!thumbnail.isNullOrBlank()) AsyncImage(thumbnail, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            if (onAdd != null) {
                FilledIconButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp)) {
                    Icon(Icons.Default.Add, "Add to playlist")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun LibraryScreen(
    playlists: List<PlaylistRow>,
    playlistThumbnails: Map<String, List<String?>>,
    playlistDescriptions: Map<String, String>,
    likedSongs: List<SongRow>,
    onOpenPlaylist: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onNewPlaylist: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineLarge)
                Text("Playlists and songs kept close.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onNewPlaylist) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("New playlist") }
        }
        Spacer(Modifier.height(28.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(190.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            item {
                LibraryPlaylistCard(
                    name = "Liked Songs",
                    subtitle = "${likedSongs.size} songs",
                    cover = null,
                    thumbnails = likedSongs.map(SongRow::thumbnailUrl),
                    onClick = onOpenLiked,
                )
            }
            items(playlists, key = PlaylistRow::id) { playlist ->
                LibraryPlaylistCard(
                    name = playlist.name,
                    subtitle = playlistDescriptions[playlist.id]?.takeIf(String::isNotBlank) ?: "Local playlist",
                    cover = playlist.thumbnailUrl,
                    thumbnails = playlistThumbnails[playlist.id].orEmpty(),
                    onClick = { onOpenPlaylist(playlist.id) },
                )
            }
        }
    }
}

@Composable
private fun LibraryPlaylistCard(
    name: String,
    subtitle: String,
    cover: String?,
    thumbnails: List<String?>,
    onClick: () -> Unit,
) {
    Column(Modifier.widthIn(max = 230.dp).clickable(onClick = onClick).padding(bottom = 14.dp)) {
        PlaylistArtwork(cover, thumbnails, 190.dp, 18.dp)
        Spacer(Modifier.height(11.dp))
        Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
private fun PlaylistDetailScreen(
    playlist: PlaylistRow,
    songs: List<SongRow>,
    songMetadata: Map<String, PlaylistSongMetadata> = emptyMap(),
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCollaborate: () -> Unit,
    eyebrow: String = "LOCAL PLAYLIST",
    description: String = "A personal collection in Hikalist",
    canManage: Boolean = true,
    canEditSongs: Boolean = true,
    downloadStates: Map<String, OfflineDownloadState> = emptyMap(),
    isDownloaded: (String) -> Boolean = { false },
    onDownloadSong: (SongRow) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onRemoveDownload: (String) -> Unit = {},
    onDownloadAll: () -> Unit = {},
    onRemoveAllDownloads: () -> Unit = {},
) {
    var menuVisible by remember { mutableStateOf(false) }
    val totalSeconds = songs.sumOf { it.duration.coerceAtLeast(0) }
    val downloadSummary = playlistDownloadSummary(songs.map(SongRow::id), downloadStates, isDownloaded)

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)).padding(36.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                    Box {
                        PlaylistArtwork(playlist.thumbnailUrl, songs.map(SongRow::thumbnailUrl), 238.dp, 20.dp)
                        if (canManage) {
                            FilledIconButton(
                                onClick = onEdit,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                            ) {
                                Icon(Icons.Default.Edit, "Change playlist cover")
                            }
                        }
                    }
                }
                Spacer(Modifier.width(28.dp))
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(playlist.name, style = MaterialTheme.typography.displayLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(10.dp))
                    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Text("${songs.size} songs • ${formatLongDuration(totalSeconds)}", fontWeight = FontWeight.SemiBold)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { if (songs.isNotEmpty()) onPlay(0) }, enabled = songs.isNotEmpty(), modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text("Play")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = onShuffle, enabled = songs.size > 1, modifier = Modifier.height(52.dp)) {
                    Icon(Icons.Default.Shuffle, null); Spacer(Modifier.width(8.dp)); Text("Shuffle")
                }
                Spacer(Modifier.width(12.dp))
                OutlinedButton(
                    onClick = onDownloadAll,
                    enabled = songs.isNotEmpty() && !downloadSummary.active && !downloadSummary.allDownloaded,
                    modifier = Modifier.height(52.dp),
                ) {
                    if (downloadSummary.active) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("${downloadSummary.completed}/${downloadSummary.total}")
                    } else {
                        Icon(
                            if (downloadSummary.allDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                            null,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (downloadSummary.allDownloaded) "Downloaded" else "Download")
                    }
                }
                Spacer(Modifier.weight(1f))
                if (canManage || downloadSummary.completed > 0) {
                    Box {
                        IconButton(onClick = { menuVisible = true }) { Icon(Icons.Default.MoreHoriz, "Playlist options") }
                        DropdownMenu(menuVisible, onDismissRequest = { menuVisible = false }) {
                            if (canManage) {
                                DropdownMenuItem(text = { Text("Collaborate") }, leadingIcon = { Icon(Icons.Default.PlaylistAdd, null) }, onClick = { menuVisible = false; onCollaborate() })
                                DropdownMenuItem(text = { Text("Edit playlist") }, leadingIcon = { Icon(Icons.Default.Edit, null) }, onClick = { menuVisible = false; onEdit() })
                                DropdownMenuItem(text = { Text("Delete playlist") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuVisible = false; onDelete() })
                            }
                            if (downloadSummary.completed > 0) {
                                DropdownMenuItem(
                                    text = { Text("Remove offline downloads") },
                                    leadingIcon = { Icon(Icons.Default.Delete, null) },
                                    onClick = { menuVisible = false; onRemoveAllDownloads() },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (songs.isEmpty()) {
            item { EmptyState("This playlist is waiting", "Use the + button on any song to add it here.") }
        } else {
            item { PlaylistTableHeader(canEditSongs) }
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                PlaylistSongRow(
                    index = index,
                    song = song,
                    metadata = songMetadata[song.id],
                    itemCount = songs.size,
                    reorderEnabled = canEditSongs,
                    onClick = { onPlay(index) },
                    onRemove = { onRemove(song.id) },
                    onMove = onMove,
                    downloadState = downloadStates[song.id],
                    downloaded = isDownloaded(song.id),
                    onDownload = { onDownloadSong(song) },
                    onCancelDownload = { onCancelDownload(song.id) },
                    onRemoveDownload = { onRemoveDownload(song.id) },
                )
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

}

@Composable
private fun PlaylistOptionsButton(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier.size(34.dp),
) {
    var menuVisible by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(
            onClick = { menuVisible = true },
            modifier = Modifier.fillMaxSize().clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        ) {
            Icon(Icons.Default.MoreHoriz, "Playlist options", Modifier.size(19.dp))
        }
        DropdownMenu(menuVisible, onDismissRequest = { menuVisible = false }) {
            DropdownMenuItem(
                text = { Text("Edit playlist") },
                leadingIcon = { Icon(Icons.Default.Edit, null) },
                onClick = { menuVisible = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text("Delete playlist") },
                leadingIcon = { Icon(Icons.Default.Delete, null) },
                onClick = { menuVisible = false; onDelete() },
            )
        }
    }
}

@Composable
private fun EditPlaylistDialog(
    playlist: PlaylistRow,
    description: String,
    songThumbnails: List<String?>,
    onDismiss: () -> Unit,
    onSave: (String, String, File?, Boolean) -> Unit,
) {
    var name by remember(playlist.id) { mutableStateOf(playlist.name) }
    var editedDescription by remember(playlist.id) { mutableStateOf(description) }
    var replacementCover by remember(playlist.id) { mutableStateOf<File?>(null) }
    var resetCover by remember(playlist.id) { mutableStateOf(false) }
    val previewCover = replacementCover?.absolutePath ?: if (resetCover) null else playlist.thumbnailUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit playlist") },
        text = {
            Column(Modifier.width(470.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaylistArtwork(previewCover, songThumbnails, 122.dp, 16.dp)
                    Spacer(Modifier.width(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            choosePlaylistCover()?.let {
                                replacementCover = it
                                resetCover = false
                            }
                        }) {
                            Icon(Icons.Default.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Choose image")
                        }
                        TextButton(
                            onClick = {
                                replacementCover = null
                                resetCover = true
                            },
                            enabled = replacementCover != null || !playlist.thumbnailUrl.isNullOrBlank(),
                        ) {
                            Text("Use automatic cover")
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = editedDescription,
                    onValueChange = { editedDescription = it.take(300) },
                    label = { Text("Description") },
                    supportingText = { Text("${editedDescription.length}/300") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), editedDescription.trim(), replacementCover, resetCover) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun DeletePlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete playlist?") },
        text = { Text("\"$playlistName\" will be removed from Hikalist. Your saved songs will not be deleted.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text("Delete") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private enum class CollaborationInviteMode { USERNAME, LINK }

private val INVITE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")

@Composable
private fun PlaylistCollaborationDialog(
    playlist: PlaylistRow,
    access: PlaylistCollaborationAccess?,
    busy: Boolean,
    message: String?,
    inviteLink: PlaylistInviteLink?,
    onDismiss: () -> Unit,
    onInvite: (String) -> Unit,
    onCreateLink: () -> Unit,
    onRevokeLink: () -> Unit,
    onRemove: (String) -> Unit,
) {
    var username by remember(playlist.id) { mutableStateOf("") }
    var inviteMode by remember(playlist.id) { mutableStateOf(CollaborationInviteMode.USERNAME) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collaborate on ${playlist.name}") },
        text = {
            Column(Modifier.width(480.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = inviteMode == CollaborationInviteMode.USERNAME,
                        onClick = { inviteMode = CollaborationInviteMode.USERNAME },
                        label = { Text("Username") },
                    )
                    FilterChip(
                        selected = inviteMode == CollaborationInviteMode.LINK,
                        onClick = { inviteMode = CollaborationInviteMode.LINK },
                        label = { Text("Invite link") },
                    )
                }
                if (inviteMode == CollaborationInviteMode.USERNAME) {
                Text(
                    "Invite an existing Hikalist username. Editors can add, remove, and reorder songs. Only you can change the name, cover, description, members, or delete this playlist.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(40) },
                    label = { Text("Hikalist username") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { onInvite(username) },
                    enabled = username.isNotBlank() && !busy,
                ) {
                    Text(if (busy) "Saving…" else "Add as editor")
                }
                } else {
                    Text(
                        "Anyone with this link can join as an editor after signing in. The link expires automatically after 2 hours.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (inviteLink == null) {
                        Button(onClick = onCreateLink, enabled = !busy) {
                            Text(if (busy) "Creating…" else "Create invite link")
                        }
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(inviteLink.shareUrl, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "Expires ${inviteLink.expiresAt.atZone(ZoneId.systemDefault()).format(INVITE_TIME_FORMAT)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { copyToClipboard(inviteLink.shareUrl) }) { Text("Copy link") }
                                    TextButton(onClick = onCreateLink, enabled = !busy) { Text("New link") }
                                    TextButton(onClick = onRevokeLink, enabled = !busy) { Text("Disable") }
                                }
                            }
                        }
                    }
                }
                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                HorizontalDivider()
                Text("People with access", style = MaterialTheme.typography.titleMedium)
                access?.collaborators.orEmpty().forEach { collaborator ->
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        collaborator.avatarUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.size(38.dp).clip(CircleShape),
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(collaborator.username, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (collaborator.role == PlaylistMemberRole.OWNER) "Owner" else "Editor",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (collaborator.role == PlaylistMemberRole.EDITOR && access?.canManagePlaylist == true) {
                            TextButton(onClick = { onRemove(collaborator.userId) }, enabled = !busy) {
                                Text("Remove")
                            }
                        }
                    }
                }
                if (access == null) {
                    Text("Loading access…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private fun copyToClipboard(value: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
}

private fun choosePlaylistCover(): File? {
    val dialog = FileDialog(null as Frame?, "Choose playlist cover", FileDialog.LOAD).apply {
        filenameFilter = java.io.FilenameFilter { _, name ->
            name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "bmp", "gif")
        }
    }
    return try {
        dialog.isVisible = true
        dialog.file?.let { File(dialog.directory, it) }
    } finally {
        dialog.dispose()
    }
}

@Composable
private fun PlaylistTableHeader(reorderEnabled: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("#", modifier = Modifier.width(34.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("TITLE", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("ALBUM", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("ADDED BY", modifier = Modifier.width(112.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("DATE ADDED", modifier = Modifier.width(116.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("TIME", modifier = Modifier.width(58.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(if (reorderEnabled) 76.dp else 44.dp))
    }
    HorizontalDivider(Modifier.padding(horizontal = 36.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun PlaylistSongRow(
    index: Int,
    song: SongRow,
    metadata: PlaylistSongMetadata?,
    itemCount: Int,
    reorderEnabled: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMove: (Int, Int) -> Unit,
    downloadState: OfflineDownloadState? = null,
    downloaded: Boolean = false,
    onDownload: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onRemoveDownload: () -> Unit = {},
) {
    var menuVisible by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 40.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", modifier = Modifier.width(34.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.weight(1.2f), verticalAlignment = Alignment.CenterVertically) {
            if (!song.thumbnailUrl.isNullOrBlank()) AsyncImage(song.thumbnailUrl, null, Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            else Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artists.orEmpty().joinToString(", ").ifBlank { "Unknown artist" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Text(song.albumName ?: "—", modifier = Modifier.weight(0.7f), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(PlaylistSongMetadataFormatter.addedBy(metadata), modifier = Modifier.width(112.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(PlaylistSongMetadataFormatter.date(metadata), modifier = Modifier.width(116.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatDuration(song.duration), modifier = Modifier.width(58.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (reorderEnabled) {
            ReorderHandle(index = index, itemCount = itemCount, itemKey = song.id, onMove = onMove)
        }
        OfflineDownloadAction(
            state = downloadState,
            downloaded = downloaded,
            onDownload = onDownload,
            onCancel = onCancelDownload,
            onRemove = onRemoveDownload,
            modifier = Modifier.size(36.dp),
        )
        Box(Modifier.width(44.dp)) {
            IconButton(onClick = { menuVisible = true }) { Icon(Icons.Default.MoreHoriz, "Song options") }
            DropdownMenu(menuVisible, onDismissRequest = { menuVisible = false }) {
                when {
                    downloadState is OfflineDownloadState.Queued || downloadState is OfflineDownloadState.Downloading -> {
                        DropdownMenuItem(
                            text = { Text("Cancel download") },
                            leadingIcon = { Icon(Icons.Default.Close, null) },
                            onClick = { menuVisible = false; onCancelDownload() },
                        )
                    }
                    downloaded -> {
                        DropdownMenuItem(
                            text = { Text("Remove download") },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            onClick = { menuVisible = false; onRemoveDownload() },
                        )
                    }
                    else -> {
                        DropdownMenuItem(
                            text = { Text(if (downloadState is OfflineDownloadState.Failed) "Retry download" else "Download") },
                            leadingIcon = { Icon(Icons.Default.Download, null) },
                            onClick = { menuVisible = false; onDownload() },
                        )
                    }
                }
                DropdownMenuItem(text = { Text("Remove from playlist") }, leadingIcon = { Icon(Icons.Default.Delete, null) }, onClick = { menuVisible = false; onRemove() })
            }
        }
    }
}

@Composable
private fun OfflineDownloadAction(
    state: OfflineDownloadState?,
    downloaded: Boolean,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier.size(36.dp),
) {
    val active = state is OfflineDownloadState.Queued || state is OfflineDownloadState.Downloading
    IconButton(
        onClick = when {
            active -> onCancel
            downloaded -> onRemove
            else -> onDownload
        },
        modifier = modifier,
    ) {
        when {
            active -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            downloaded -> Icon(Icons.Default.DownloadDone, "Remove offline download", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            state is OfflineDownloadState.Failed -> Icon(Icons.Default.ErrorOutline, "Retry download", Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            else -> Icon(Icons.Default.Download, "Download for offline listening", Modifier.size(20.dp))
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PlayerBar(
    controller: HikalistController,
    settings: AppSettings,
    onOpenSong: () -> Unit,
    onOpenLyrics: () -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    onToggleQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val player = controller.player
    val current by player.currentSong.collectAsState()
    val state by player.state.collectAsState()
    val position by player.position.collectAsState()
    val duration by player.duration.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val downloadStates by controller.downloadStates.collectAsState()
    val persistedVolume by settings.volumePercent.collectAsState()
    var volume by remember { mutableStateOf(persistedVolume / 100f) }
    var volumeAdjusting by remember { mutableStateOf(false) }
    var seekValue by remember { mutableStateOf(position.toFloat()) }
    var seeking by remember { mutableStateOf(false) }
    val isPlaying = state == DesktopMusicPlayer.State.PLAYING || state == DesktopMusicPlayer.State.BUFFERING

    LaunchedEffect(position) {
        if (!seeking) seekValue = position.toFloat()
    }
    LaunchedEffect(persistedVolume) {
        if (!volumeAdjusting) {
            volume = persistedVolume / 100f
            player.setVolume(persistedVolume)
        }
    }

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(108.dp)) {
            val compact = maxWidth < 1100.dp
            Row(
                Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).clickable(enabled = current != null, onClick = onOpenSong).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!current?.thumbnailUrl.isNullOrBlank()) AsyncImage(current?.thumbnailUrl, null, Modifier.size(58.dp).clip(RoundedCornerShape(9.dp)), contentScale = ContentScale.Crop)
                        else PlaylistArtwork(null, emptyList(), 58.dp, 9.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(current?.title ?: "Nothing playing", fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(current?.artist ?: "Choose a song to begin", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    current?.item?.let { item ->
                        IconButton(onClick = onOpenLyrics, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Mic, "Lyrics", Modifier.size(19.dp)) }
                        IconButton(onClick = { controller.toggleLike(item) }, modifier = Modifier.size(36.dp)) {
                            Icon(if (controller.isLiked(item.videoId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Liked Songs", Modifier.size(19.dp), tint = if (controller.isLiked(item.videoId)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onAddToPlaylist(item) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "Add to playlist", Modifier.size(20.dp)) }
                        OfflineDownloadAction(
                            state = downloadStates[item.videoId],
                            downloaded = controller.isDownloaded(item.videoId),
                            onDownload = { controller.download(item) },
                            onCancel = { controller.cancelDownload(item.videoId) },
                            onRemove = { controller.removeDownload(item.videoId) },
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }

                Column(
                    Modifier.width(if (compact) 390.dp else 510.dp).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { player.setShuffle(!shuffle) }, modifier = Modifier.size(34.dp)) { Icon(Icons.Default.Shuffle, "Shuffle", Modifier.size(19.dp), tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = player::skipPrevious, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(22.dp)) }
                        FilledIconButton(onClick = player::playPause, enabled = current != null, modifier = Modifier.size(44.dp)) {
                            if (state == DesktopMusicPlayer.State.BUFFERING) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                            else Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(23.dp))
                        }
                        IconButton(onClick = player::skipNext, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(22.dp)) }
                        IconButton(onClick = { player.cycleRepeatMode(); settings.setRepeat(player.repeatMode.value.toPreference()) }, modifier = Modifier.size(34.dp)) {
                            Icon(if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", Modifier.size(19.dp), tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(formatDuration(position.toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp))
                        HoverSeekSlider(
                            value = seekValue,
                            duration = duration.toFloat().coerceAtLeast(1f),
                            onValueChange = { seeking = true; seekValue = it },
                            onValueChangeFinished = { player.seekTo(seekValue); seeking = false },
                            modifier = Modifier.weight(1f),
                        )
                        Text(formatDuration(duration.toInt()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(42.dp), maxLines = 1)
                    }
                }

                Row(
                    Modifier.width(if (compact) 178.dp else 220.dp).padding(end = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onToggleQueue, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.QueueMusic, "Queue", Modifier.size(20.dp)) }
                    Icon(Icons.Default.VolumeUp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(7.dp))
                    ThinSlider(
                        value = volume,
                        onValueChange = {
                            volumeAdjusting = true
                            volume = it
                            player.setVolume((it * 100).toInt())
                        },
                        onValueChangeFinished = {
                            settings.setVolumePercent((volume * 100).toInt())
                            volumeAdjusting = false
                        },
                        modifier = Modifier.width(if (compact) 64.dp else 84.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("${(volume * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(34.dp), maxLines = 1)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
private fun HoverSeekSlider(
    value: Float,
    duration: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovered by remember { mutableStateOf(false) }
    var hoverX by remember { mutableStateOf(0f) }
    var widthPx by remember { mutableStateOf(1) }
    val density = LocalDensity.current
    val hoverTime = (hoverX / widthPx.coerceAtLeast(1) * duration).coerceIn(0f, duration)

    BoxWithConstraints(
        modifier.height(34.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .onPointerEvent(PointerEventType.Enter) { event ->
                hovered = true
                hoverX = event.changes.firstOrNull()?.position?.x ?: 0f
            }
            .onPointerEvent(PointerEventType.Move) { event ->
                hoverX = event.changes.firstOrNull()?.position?.x ?: hoverX
            }
            .onPointerEvent(PointerEventType.Exit) { hovered = false },
    ) {
        if (hovered && duration > 1f) {
            val tooltipWidth = 48.dp
            val rawX = with(density) { hoverX.toDp() - tooltipWidth / 2 }
            val tooltipX = rawX.coerceIn(0.dp, (maxWidth - tooltipWidth).coerceAtLeast(0.dp))
            Surface(
                color = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.offset(x = tooltipX).width(tooltipWidth).height(20.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(formatDuration(hoverTime.toInt()), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        ThinSlider(
            value = value.coerceIn(0f, duration),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..duration,
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.onSurface,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
    )
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        modifier = modifier.height(18.dp),
        colors = colors,
        thumb = {
            Box(
                Modifier.offset(y = PlayerUiGeometry.sliderThumbCorrectionDp.dp).size(9.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(3.dp),
                colors = colors,
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}

@Composable
private fun SongScreen(
    controller: HikalistController,
    settings: AppSettings,
    queueVisible: Boolean,
    onToggleQueue: () -> Unit,
    onClose: () -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    modifier: Modifier,
) {
    val player = controller.player
    val current by player.currentSong.collectAsState()
    val position by player.position.collectAsState()
    val duration by player.duration.collectAsState()
    val state by player.state.collectAsState()
    val queue by player.queueItems.collectAsState()
    val queueIndex by player.queueIndex.collectAsState()
    val shuffle by player.shuffle.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()
    val volumePercent by player.volumePercent.collectAsState()
    val downloadStates by controller.downloadStates.collectAsState()
    var lyrics by remember(current?.item?.videoId) { mutableStateOf<List<LyricLine>>(emptyList()) }
    var lyricsLoading by remember(current?.item?.videoId) { mutableStateOf(current != null) }
    var lyricsUnavailable by remember(current?.item?.videoId) { mutableStateOf(false) }
    var showLyrics by remember(current?.item?.videoId) { mutableStateOf(settings.showLyricsByDefault.value) }
    var seekValue by remember { mutableStateOf(position.toFloat()) }
    var seeking by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(volumePercent / 100f) }
    var volumeAdjusting by remember { mutableStateOf(false) }
    var volumePopupVisible by remember { mutableStateOf(false) }

    LaunchedEffect(position) {
        if (!seeking) seekValue = position.toFloat()
    }
    LaunchedEffect(volumePercent) {
        if (!volumeAdjusting) volume = volumePercent / 100f
    }

    LaunchedEffect(current?.item?.videoId) {
        val item = current?.item ?: return@LaunchedEffect
        lyricsLoading = true
        lyricsUnavailable = false
        val lyricText = withContext(Dispatchers.IO) {
            controller.lyricsResolver.resolve(
                LyricsRequest(
                    videoId = item.videoId,
                    title = item.title,
                    artist = item.artist,
                    durationSeconds = item.duration.takeIf { it > 0 } ?: duration.toInt(),
                    album = item.album,
                ),
            )
        }
        lyrics = lyricText?.let(SyncedLyricsParser::parse).orEmpty()
        lyricsUnavailable = lyrics.isEmpty()
        lyricsLoading = false
    }

    Column(modifier.background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Default.ExpandMore, "Back") }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleQueue) { Icon(Icons.Default.QueueMusic, "Queue", tint = if (queueVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 42.dp, vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(42.dp)) {
            Column(Modifier.width(390.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (!current?.thumbnailUrl.isNullOrBlank()) AsyncImage(current?.thumbnailUrl, null, Modifier.size(330.dp).clip(RoundedCornerShape(24.dp)), contentScale = ContentScale.Crop)
                else PlaylistArtwork(null, emptyList(), 330.dp, 24.dp)
                Spacer(Modifier.height(24.dp))
                Text(current?.title ?: "Nothing playing", style = MaterialTheme.typography.headlineMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(current?.artist.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                current?.item?.let { item ->
                    Spacer(Modifier.height(10.dp))
                    Row {
                        IconButton(onClick = { showLyrics = !showLyrics }) { Icon(Icons.Default.Mic, "Lyrics", tint = if (showLyrics) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { controller.toggleLike(item) }) { Icon(if (controller.isLiked(item.videoId)) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Liked Songs", tint = if (controller.isLiked(item.videoId)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { onAddToPlaylist(item) }) { Icon(Icons.Default.PlaylistAdd, "Add to playlist") }
                        OfflineDownloadAction(
                            state = downloadStates[item.videoId],
                            downloaded = controller.isDownloaded(item.videoId),
                            onDownload = { controller.download(item) },
                            onCancel = { controller.cancelDownload(item.videoId) },
                            onRemove = { controller.removeDownload(item.videoId) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDuration(position.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp),
                    )
                    HoverSeekSlider(
                        value = seekValue,
                        duration = duration.toFloat().coerceAtLeast(1f),
                        onValueChange = { seeking = true; seekValue = it },
                        onValueChangeFinished = { player.seekTo(seekValue); seeking = false },
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        formatDuration(duration.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp),
                        maxLines = 1,
                    )
                    Box {
                        IconButton(
                            onClick = { volumePopupVisible = true },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                "Volume",
                                Modifier.size(19.dp),
                                tint = if (volumePopupVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = volumePopupVisible,
                            onDismissRequest = { volumePopupVisible = false },
                            modifier = Modifier.width(230.dp),
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VolumeUp, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Volume", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.weight(1f))
                                    Text(
                                        "${(volume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                ThinSlider(
                                    value = volume,
                                    onValueChange = {
                                        volumeAdjusting = true
                                        volume = it
                                        player.setVolume((it * 100).toInt())
                                    },
                                    onValueChangeFinished = {
                                        settings.setVolumePercent((volume * 100).toInt())
                                        volumeAdjusting = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { player.setShuffle(!shuffle) }) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = player::skipPrevious) { Icon(Icons.Default.SkipPrevious, "Previous", Modifier.size(32.dp)) }
                    FilledIconButton(onClick = player::playPause, modifier = Modifier.size(64.dp)) {
                        if (state == DesktopMusicPlayer.State.BUFFERING) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        else Icon(if (state == DesktopMusicPlayer.State.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow, "Play or pause", Modifier.size(30.dp))
                    }
                    IconButton(onClick = player::skipNext) { Icon(Icons.Default.SkipNext, "Next", Modifier.size(32.dp)) }
                    IconButton(onClick = { player.cycleRepeatMode(); settings.setRepeat(player.repeatMode.value.toPreference()) }) { Icon(if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (repeatMode == RepeatMode.OFF) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary) }
                }
            }

            AnimatedVisibility(showLyrics, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.weight(1f).fillMaxHeight()) {
                LyricsPanel(lyrics, lyricsLoading, lyricsUnavailable, position, onSeek = { player.seekTo(it / 1000f) })
            }
            AnimatedVisibility(queueVisible, modifier = Modifier.width(320.dp).fillMaxHeight()) {
                QueuePanel(
                    items = queue,
                    currentIndex = queueIndex,
                    onSelect = player::selectQueueItem,
                    onRemove = player::removeQueueItem,
                    onMove = player::moveQueueItem,
                    onPlayNext = player::playQueueItemNext,
                    onClear = player::clearUpcoming,
                )
            }
        }
    }
}

@Composable
private fun LyricsPanel(
    lines: List<LyricLine>,
    loading: Boolean,
    unavailable: Boolean,
    positionSeconds: Double,
    onSeek: (Long) -> Unit,
) {
    val listState = rememberLazyListState()
    val positionMs = (positionSeconds * 1000).toLong()
    val activeIndex = SyncedLyricsParser.activeIndex(lines, positionMs)
    val waitingForFirstLine = SyncedLyricsParser.isWaitingForFirstTimedLine(lines, positionMs)
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f), shape = RoundedCornerShape(24.dp)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            unavailable -> EmptyState("Lyrics unavailable", "This song does not have matching lyrics yet.")
            else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 38.dp, vertical = 44.dp), state = listState, verticalArrangement = Arrangement.spacedBy(22.dp)) {
                item(key = "lyrics-prelude") {
                    LyricsPreludeIndicator(visible = waitingForFirstLine)
                }
                itemsIndexed(lines) { index, line ->
                    val active = index == activeIndex
                    val scale by animateFloatAsState(
                        targetValue = if (active) 1.08f else 1f,
                        animationSpec = spring(dampingRatio = 0.76f, stiffness = 320f),
                        label = "lyric-scale",
                    )
                    val alpha by animateFloatAsState(
                        targetValue = when {
                            active -> 1f
                            activeIndex >= 0 && index < activeIndex -> 0.34f
                            else -> 0.56f
                        },
                        animationSpec = tween(durationMillis = 360),
                        label = "lyric-alpha",
                    )
                    val color by animateColorAsState(
                        targetValue = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(durationMillis = 360),
                        label = "lyric-color",
                    )
                    Text(
                        line.text,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 25.sp),
                        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                        color = color,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                this.alpha = alpha
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = line.timeMs != null) { line.timeMs?.let(onSeek) }
                            .padding(vertical = 2.dp),
                    )
                }
                item { Spacer(Modifier.height(180.dp)) }
            }
        }
    }
}

@Composable
private fun LyricsPreludeIndicator(visible: Boolean) {
    val pulse = rememberInfiniteTransition(label = "lyrics-prelude-pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.42f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720),
            repeatMode = AnimationRepeatMode.Reverse,
        ),
        label = "lyrics-prelude-alpha",
    )
    val visibilityAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "lyrics-prelude-visibility",
    )

    Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = "•••",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer { alpha = visibilityAlpha * pulseAlpha },
        )
    }
}

@Composable
private fun QueuePanel(
    items: List<DesktopMusicPlayer.QueueItem>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onPlayNext: (Int) -> Unit,
    onClear: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Queue", style = MaterialTheme.typography.titleLarge)
                    Text("${items.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onClear) { Icon(Icons.Default.ClearAll, "Clear upcoming") }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(items, key = { _, item -> item.videoId }) { index, item ->
                    var menuVisible by remember(item.videoId) { mutableStateOf(false) }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
                            .background(if (index == currentIndex) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f) else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onSelect(index) }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!item.thumbnailUrl.isNullOrBlank()) AsyncImage(item.thumbnailUrl, null, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                        else Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Medium)
                            Text(item.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        ReorderHandle(index, items.size, item.videoId, onMove, Modifier.size(30.dp))
                        if (index != currentIndex) {
                            Box {
                                IconButton(onClick = { menuVisible = true }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.MoreHoriz, "Queue options", Modifier.size(18.dp))
                                }
                                DropdownMenu(menuVisible, onDismissRequest = { menuVisible = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Play next") },
                                        leadingIcon = { Icon(Icons.Default.PlaylistPlay, null) },
                                        onClick = { menuVisible = false; onPlayNext(index) },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove from queue") },
                                        leadingIcon = { Icon(Icons.Default.Delete, null) },
                                        onClick = { menuVisible = false; onRemove(index) },
                                    )
                                }
                            }
                        } else {
                            Spacer(Modifier.width(30.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReorderHandle(
    index: Int,
    itemCount: Int,
    itemKey: String,
    onMove: (Int, Int) -> Unit,
    modifier: Modifier = Modifier.size(32.dp),
) {
    val latestIndex by rememberUpdatedState(index)
    val rowThreshold = with(LocalDensity.current) { 48.dp.toPx() }
    Icon(
        imageVector = Icons.Default.DragHandle,
        contentDescription = "Hold and drag to reorder",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(5.dp).pointerInput(itemKey, itemCount) {
            var draggedIndex = latestIndex
            var accumulatedDrag = 0f
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    draggedIndex = latestIndex
                    accumulatedDrag = 0f
                },
                onDragCancel = { accumulatedDrag = 0f },
                onDragEnd = { accumulatedDrag = 0f },
                onDrag = { change, dragAmount ->
                    change.consume()
                    accumulatedDrag += dragAmount.y
                    while (accumulatedDrag >= rowThreshold && draggedIndex < itemCount - 1) {
                        onMove(draggedIndex, draggedIndex + 1)
                        draggedIndex += 1
                        accumulatedDrag -= rowThreshold
                    }
                    while (accumulatedDrag <= -rowThreshold && draggedIndex > 0) {
                        onMove(draggedIndex, draggedIndex - 1)
                        draggedIndex -= 1
                        accumulatedDrag += rowThreshold
                    }
                },
            )
        },
    )
}

@Composable
private fun AddToPlaylistDialog(
    item: DesktopMusicPlayer.QueueItem,
    playlists: List<PlaylistRow>,
    playlistAccess: Map<String, PlaylistCollaborationAccess>,
    onDismiss: () -> Unit,
    onCreatePlaylist: (String) -> PlaylistRow,
    onAdd: (String) -> Unit,
) {
    var createMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (createMode) "New playlist" else "Add to playlist") },
        text = {
            if (createMode) {
                OutlinedTextField(name, onValueChange = { name = it }, label = { Text("Playlist name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            } else {
                Column(Modifier.height(330.dp)) {
                    OutlinedButton(onClick = { createMode = true }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create new playlist") }
                    Spacer(Modifier.height(10.dp))
                    LazyColumn {
                        items(playlists, key = PlaylistRow::id) { playlist ->
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onAdd(playlist.id) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                PlaylistArtwork(playlist.thumbnailUrl, emptyList(), 46.dp, 9.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(playlist.name, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        playlistStatusLabel(playlist, playlistAccess[playlist.id]),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (createMode) Button(onClick = {
                val playlist = onCreatePlaylist(name)
                onAdd(playlist.id)
            }, enabled = name.isNotBlank()) { Text("Create and add") }
        },
        dismissButton = { TextButton(onClick = if (createMode) ({ createMode = false }) else onDismiss) { Text(if (createMode) "Back" else "Cancel") } },
    )
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        HikalistLogo(86.dp)
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatDuration(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}

private fun formatLongDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours} hr ${minutes} min" else "${minutes} min"
}
