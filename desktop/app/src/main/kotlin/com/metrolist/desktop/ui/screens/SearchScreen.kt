package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.metrolist.desktop.ui.toQueueItem
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.YTItem
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    onOpenContent: (YTItem) -> Unit,
    onSearchRecorded: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(SearchCategory.ALL) }
    var results by remember { mutableStateOf<List<YTItem>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, category) {
        if (query.trim().length < 2) {
            results = emptyList()
            loading = false
            errorMessage = null
            return@LaunchedEffect
        }
        delay(350)
        loading = true
        errorMessage = null
        runCatching { loadSearchResults(query.trim(), category) }
            .onSuccess { loaded ->
                results = loaded
                if (loaded.isNotEmpty()) onSearchRecorded(query.trim())
            }
            .onFailure {
                results = emptyList()
                errorMessage = "Search could not be loaded. Check your connection and try again."
            }
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 28.dp)) {
        Text("Search", style = MaterialTheme.typography.headlineLarge)
        Text("Find songs, albums, artists, and playlists", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Song, artist, album, or playlist") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            trailingIcon = {
                if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                    Icon(Icons.Default.Clear, "Clear")
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SearchCategory.entries.forEach { item ->
                FilterChip(
                    selected = category == item,
                    onClick = { category = item },
                    label = { Text(item.label) },
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            errorMessage != null -> SearchMessage("Search unavailable", errorMessage.orEmpty())
            query.trim().length < 2 -> SearchMessage("What do you want to hear?", "Type at least two characters to start searching.")
            results.isEmpty() -> SearchMessage("No results", "Try another title, artist, album, or playlist name.")
            else -> SearchResults(results, onPlay, onAddToPlaylist, onOpenContent)
        }
    }
}

@Composable
private fun SearchResults(
    results: List<YTItem>,
    onPlay: (List<DesktopMusicPlayer.QueueItem>, Int) -> Unit,
    onAddToPlaylist: (DesktopMusicPlayer.QueueItem) -> Unit,
    onOpenContent: (YTItem) -> Unit,
) {
    val songs = results.filterIsInstance<SongItem>()
    LazyColumn(Modifier.fillMaxSize()) {
        items(results, key = { "${it::class.simpleName}-${it.id}" }) { item ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .clickable {
                        if (item is SongItem) {
                            val index = songs.indexOfFirst { it.id == item.id }
                            onPlay(songs.map(SongItem::toQueueItem), index.coerceAtLeast(0))
                        } else {
                            onOpenContent(item)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!item.thumbnail.isNullOrBlank()) {
                    AsyncImage(item.thumbnail, null, Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                } else {
                    Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.searchSubtitle(), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(item.typeLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(88.dp))
                if (item is SongItem) {
                    Text(formatDuration(item.duration ?: 0), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(58.dp))
                    IconButton(onClick = { onAddToPlaylist(item.toQueueItem()) }) {
                        Icon(Icons.Default.Add, "Add to playlist")
                    }
                } else {
                    Spacer(Modifier.width(106.dp))
                }
            }
        }
    }
}

@Composable
private fun SearchMessage(title: String, message: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private suspend fun loadSearchResults(query: String, category: SearchCategory): List<YTItem> {
    val items = when (category) {
        SearchCategory.ALL -> YouTube.searchSummary(query).getOrThrow().summaries.flatMap { it.items }
        SearchCategory.SONGS -> YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow().items
        SearchCategory.ALBUMS -> YouTube.search(query, YouTube.SearchFilter.FILTER_ALBUM).getOrThrow().items
        SearchCategory.ARTISTS -> YouTube.search(query, YouTube.SearchFilter.FILTER_ARTIST).getOrThrow().items
        SearchCategory.PLAYLISTS ->
            YouTube.search(query, YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST).getOrThrow().items +
                YouTube.search(query, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST).getOrThrow().items
    }
    return items.filter(category::accepts).distinctBy { "${it::class.simpleName}-${it.id}" }
}

private fun YTItem.searchSubtitle(): String = when (this) {
    is SongItem -> artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" }
    is AlbumItem -> listOfNotNull(artists?.joinToString(", ") { it.name }, year?.toString()).joinToString(" • ").ifBlank { "Album" }
    is ArtistItem -> "Artist"
    is PlaylistItem -> listOfNotNull(author?.name, songCountText).joinToString(" • ").ifBlank { "Playlist" }
    else -> "Music"
}

private fun YTItem.typeLabel(): String = when (this) {
    is SongItem -> "Song"
    is AlbumItem -> "Album"
    is ArtistItem -> "Artist"
    is PlaylistItem -> "Playlist"
    else -> "Music"
}

private fun formatDuration(seconds: Int): String = "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
