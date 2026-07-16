package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.desktop.player.DesktopMusicPlayer
import com.metrolist.desktop.ui.ContentDestination
import com.metrolist.desktop.ui.contentDestinationFor
import com.metrolist.desktop.ui.toQueueItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.pages.AlbumPage
import com.metrolist.innertube.pages.ArtistPage
import com.metrolist.innertube.pages.PlaylistPage

@Composable
fun ContentDetailScreen(
    destination: ContentDestination,
    onBack: () -> Unit,
    onOpenContent: (YTItem) -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    when (destination) {
        is ContentDestination.Album -> AlbumDetailScreen(destination.id, onBack, onOpenContent, onPlay, onShuffle, onAddToPlaylist)
        is ContentDestination.Artist -> ArtistDetailScreen(destination.id, onBack, onOpenContent, onPlay, onShuffle, onAddToPlaylist)
        is ContentDestination.Playlist -> PlaylistDetailScreen(destination.id, onBack, onOpenContent, onPlay, onShuffle, onAddToPlaylist)
    }
}

@Composable
private fun AlbumDetailScreen(
    albumId: String,
    onBack: () -> Unit,
    onOpenContent: (YTItem) -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    var page by remember(albumId) { mutableStateOf<AlbumPage?>(null) }
    var error by remember(albumId) { mutableStateOf(false) }
    LaunchedEffect(albumId) {
        page = YouTube.album(albumId).onFailure { error = true }.getOrNull()
    }
    RemoteLoadState(page, error, onBack) { albumPage ->
        val album = albumPage.album
        val subtitle = listOfNotNull(album.artists?.joinToString(", ") { it.name }, album.year?.toString())
            .joinToString(" • ")
        RemoteCollection(
            type = "ALBUM",
            title = album.title,
            subtitle = subtitle,
            description = "${albumPage.songs.size} songs • ${formatTotalDuration(albumPage.songs)}",
            thumbnail = album.thumbnail,
            songs = albumPage.songs,
            onBack = onBack,
            onPlay = onPlay,
            onShuffle = onShuffle,
            onAddToPlaylist = onAddToPlaylist,
            footer = if (albumPage.otherVersions.isEmpty()) null else ({
                ContentShelf("Other versions", albumPage.otherVersions, onOpenContent, onPlay, onAddToPlaylist)
            }),
        )
    }
}

@Composable
private fun PlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onOpenContent: (YTItem) -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    var page by remember(playlistId) { mutableStateOf<PlaylistPage?>(null) }
    var songs by remember(playlistId) { mutableStateOf<List<SongItem>>(emptyList()) }
    var loadingMore by remember(playlistId) { mutableStateOf(false) }
    var error by remember(playlistId) { mutableStateOf(false) }
    LaunchedEffect(playlistId) {
        val firstPage = YouTube.playlist(playlistId).onFailure { error = true }.getOrNull() ?: return@LaunchedEffect
        page = firstPage
        songs = firstPage.songs
        var continuation = firstPage.songsContinuation
        var requestCount = 0
        val seenContinuations = mutableSetOf<String>()
        loadingMore = continuation != null
        while (continuation != null && requestCount < 30 && seenContinuations.add(continuation)) {
            val next = YouTube.playlistContinuation(continuation).getOrNull() ?: break
            songs = (songs + next.songs).distinctBy(SongItem::id)
            continuation = next.continuation
            requestCount += 1
        }
        loadingMore = false
    }
    RemoteLoadState(page, error, onBack) { playlistPage ->
        val playlist = playlistPage.playlist
        val author = playlist.author
        RemoteCollection(
            type = "PUBLIC PLAYLIST",
            title = playlist.title,
            subtitle = playlist.author?.name ?: "YouTube Music",
            description = playlist.description?.takeIf(String::isNotBlank)
                ?: "${songs.size} songs • ${formatTotalDuration(songs)}",
            thumbnail = playlist.thumbnail,
            songs = songs,
            onBack = onBack,
            onPlay = onPlay,
            onShuffle = onShuffle,
            onAddToPlaylist = onAddToPlaylist,
            loadingMore = loadingMore,
            footer = author?.id?.let { artistId ->
                {
                    Text(
                        "More from ${author.name}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onOpenContent(ArtistItem(artistId, author.name, playlist.authorAvatarUrl, shuffleEndpoint = null, radioEndpoint = null)) }
                            .padding(vertical = 18.dp),
                    )
                }
            },
        )
    }
}

@Composable
private fun ArtistDetailScreen(
    artistId: String,
    onBack: () -> Unit,
    onOpenContent: (YTItem) -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    var page by remember(artistId) { mutableStateOf<ArtistPage?>(null) }
    var error by remember(artistId) { mutableStateOf(false) }
    LaunchedEffect(artistId) {
        page = YouTube.artist(artistId).onFailure { error = true }.getOrNull()
    }
    RemoteLoadState(page, error, onBack) { artistPage ->
        val allSongs = artistPage.sections.flatMap { it.items }.filterIsInstance<SongItem>().distinctBy(SongItem::id)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                ArtistHeader(artistPage, allSongs, onBack, onPlay, onShuffle)
            }
            artistPage.sections.forEach { section ->
                val supportedItems = section.items.filter { it is SongItem || contentDestinationFor(it) != null }
                if (supportedItems.isNotEmpty()) {
                    item {
                        ContentShelf(section.title.ifBlank { "Featured" }, supportedItems, onOpenContent, onPlay, onAddToPlaylist)
                    }
                }
            }
            artistPage.description?.takeIf(String::isNotBlank)?.let { description ->
                item {
                    Column(Modifier.padding(horizontal = 36.dp, vertical = 26.dp)) {
                        Text("About", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun ArtistHeader(
    page: ArtistPage,
    songs: List<SongItem>,
    onBack: () -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)).padding(36.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
        Row(verticalAlignment = Alignment.Bottom) {
            Artwork(page.artist.thumbnail, 220.dp, CircleShape)
            Spacer(Modifier.width(28.dp))
            Column(Modifier.weight(1f)) {
                Text("ARTIST", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(page.artist.title, style = MaterialTheme.typography.displayLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val listenerText = page.monthlyListenerCount ?: page.subscriberCountText
                if (!listenerText.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(listenerText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(18.dp))
                PlaybackButtons(songs, onPlay, onShuffle)
            }
        }
    }
}

@Composable
private fun RemoteCollection(
    type: String,
    title: String,
    subtitle: String,
    description: String,
    thumbnail: String?,
    songs: List<SongItem>,
    onBack: () -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    loadingMore: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)).padding(36.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                Row(verticalAlignment = Alignment.Bottom) {
                    Artwork(thumbnail, 230.dp, RoundedCornerShape(20.dp))
                    Spacer(Modifier.width(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Text(title, style = MaterialTheme.typography.displayLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (subtitle.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text(subtitle, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(18.dp))
                        PlaybackButtons(songs, onPlay, onShuffle)
                    }
                }
            }
        }
        if (songs.isEmpty() && !loadingMore) {
            item { RemoteMessage("No playable songs", "This collection does not currently expose any songs.") }
        } else {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 12.dp)) {
                    Text("#", Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TITLE", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ALBUM", Modifier.width(210.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("TIME", Modifier.width(62.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(48.dp))
                }
                HorizontalDivider(Modifier.padding(horizontal = 36.dp), color = MaterialTheme.colorScheme.outlineVariant)
            }
            items(songs, key = SongItem::id) { song ->
                val index = songs.indexOfFirst { it.id == song.id }
                RemoteSongRow(song, index, { onPlay(songs.map(SongItem::toQueueItem), index) }, onAddToPlaylist)
            }
            if (loadingMore) {
                item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(26.dp)) } }
            }
        }
        footer?.let { item { Column(Modifier.padding(horizontal = 36.dp)) { it() } } }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun PlaybackButtons(
    songs: List<SongItem>,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onShuffle: (List<DesktopMusicPlayer.QueueItem>) -> Unit,
) {
    val queue = songs.map(SongItem::toQueueItem)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { onPlay(queue, 0) }, enabled = queue.isNotEmpty()) {
            Icon(Icons.Default.PlayArrow, null)
            Spacer(Modifier.width(7.dp))
            Text("Play")
        }
        OutlinedButton(onClick = { onShuffle(queue) }, enabled = queue.size > 1) {
            Icon(Icons.Default.Shuffle, null)
            Spacer(Modifier.width(7.dp))
            Text("Shuffle")
        }
    }
}

@Composable
private fun RemoteSongRow(
    song: SongItem,
    index: Int,
    onClick: () -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 40.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${index + 1}", Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Artwork(song.thumbnail, 48.dp, RoundedCornerShape(8.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(song.artists.joinToString(", ") { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(song.album?.name.orEmpty(), Modifier.width(210.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatDuration(song.duration ?: 0), Modifier.width(62.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconButton(onClick = { onAddToPlaylist(song.toQueueItem()) }) { Icon(Icons.Default.Add, "Add to playlist") }
    }
}

@Composable
private fun ContentShelf(
    title: String,
    items: List<YTItem>,
    onOpenContent: (YTItem) -> Unit,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 20.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        val songs = items.filterIsInstance<SongItem>()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(items, key = { "${it::class.simpleName}-${it.id}" }) { item ->
                Column(
                    Modifier.width(168.dp).clip(RoundedCornerShape(14.dp)).clickable {
                        if (item is SongItem) {
                            val index = songs.indexOfFirst { it.id == item.id }
                            onPlay(songs.map(SongItem::toQueueItem), index.coerceAtLeast(0))
                        } else {
                            onOpenContent(item)
                        }
                    },
                ) {
                    Box {
                        Artwork(item.thumbnail, 168.dp, RoundedCornerShape(14.dp))
                        if (item is SongItem) {
                            FilledIconButton(
                                onClick = { onAddToPlaylist(item.toQueueItem()) },
                                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).size(38.dp),
                            ) { Icon(Icons.Default.Add, "Add to playlist") }
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(item.contentSubtitle(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun <T> RemoteLoadState(value: T?, error: Boolean, onBack: () -> Unit, content: @Composable (T) -> Unit) {
    when {
        value != null -> content(value)
        error -> Column(Modifier.fillMaxSize()) {
            IconButton(onClick = onBack, modifier = Modifier.padding(24.dp)) { Icon(Icons.Default.ArrowBack, "Back") }
            RemoteMessage("Could not load this page", "Check your connection and try opening it again.")
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    }
}

@Composable
private fun RemoteMessage(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Artwork(thumbnail: String?, size: androidx.compose.ui.unit.Dp, shape: androidx.compose.ui.graphics.Shape) {
    if (!thumbnail.isNullOrBlank()) {
        AsyncImage(thumbnail, null, Modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
    } else {
        Box(Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private fun YTItem.contentSubtitle(): String = when (this) {
    is SongItem -> artists.joinToString(", ") { it.name }
    is AlbumItem -> listOfNotNull(artists?.joinToString(", ") { it.name }, year?.toString()).joinToString(" • ")
    is ArtistItem -> "Artist"
    is PlaylistItem -> author?.name ?: "Playlist"
    else -> "Music"
}

private fun formatDuration(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"

private fun formatTotalDuration(songs: List<SongItem>): String {
    val seconds = songs.sumOf { it.duration ?: 0 }
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours} hr ${minutes} min" else "${minutes} min"
}
