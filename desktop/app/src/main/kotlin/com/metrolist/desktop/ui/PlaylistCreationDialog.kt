package com.metrolist.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.desktop.spotify.SpotifyImportParser
import com.metrolist.desktop.spotify.SpotifyImportResult
import com.metrolist.desktop.spotify.SpotifyPlaylistImportService
import com.metrolist.innertube.models.SongItem
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.io.File
import kotlinx.coroutines.launch

private enum class PlaylistCreationMode { EMPTY, SPOTIFY }

private val spotifyPlaylistImporter = SpotifyPlaylistImportService()

@Composable
fun PlaylistCreationDialog(
    onDismiss: () -> Unit,
    onCreateEmpty: (String) -> Unit,
    onImportSpotify: (String, String, List<SongItem>) -> Unit,
) {
    var mode by remember { mutableStateOf(PlaylistCreationMode.EMPTY) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sourceText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SpotifyImportResult>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var completed by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val parseResult = remember(sourceText) { SpotifyImportParser.parse(sourceText) }
    val matchedSongs = results.orEmpty().mapNotNull(SpotifyImportResult::song).distinctBy(SongItem::id)

    fun findSongs() {
        if (parseResult.tracks.isEmpty()) {
            feedback = "Tempel link lagu Spotify atau pilih file .txt terlebih dahulu."
            return
        }
        loading = true
        feedback = null
        results = null
        completed = 0
        total = parseResult.tracks.size
        scope.launch {
            results = spotifyPlaylistImporter.import(parseResult.tracks) { done, count ->
                completed = done
                total = count
            }
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (mode == PlaylistCreationMode.EMPTY) "Create playlist" else "Import from Spotify") },
        text = {
            Column(
                Modifier.width(680.dp).heightIn(max = 670.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = mode == PlaylistCreationMode.EMPTY,
                        onClick = { if (!loading) mode = PlaylistCreationMode.EMPTY },
                        label = { Text("Empty playlist") },
                    )
                    FilterChip(
                        selected = mode == PlaylistCreationMode.SPOTIFY,
                        onClick = { if (!loading) mode = PlaylistCreationMode.SPOTIFY },
                        label = { Text("Import Spotify") },
                        leadingIcon = { Icon(Icons.Default.LibraryMusic, null, Modifier.size(18.dp)) },
                    )
                }

                if (mode == PlaylistCreationMode.EMPTY) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(100) },
                        label = { Text("Playlist name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (results == null) {
                    SpotifySourceStep(
                        sourceText = sourceText,
                        onSourceChange = {
                            sourceText = it
                            feedback = null
                        },
                        trackCount = parseResult.tracks.size,
                        duplicateCount = parseResult.duplicateEntries,
                        ignoredCount = parseResult.ignoredEntries,
                        loading = loading,
                        completed = completed,
                        total = total,
                        onReadClipboard = {
                            runCatching(::readSpotifyClipboard)
                                .onSuccess {
                                    sourceText = it
                                    feedback = null
                                }
                                .onFailure { feedback = it.message ?: "Clipboard tidak dapat dibaca." }
                        },
                        onChooseFile = {
                            chooseSpotifyTextFile()?.let { file ->
                                runCatching { file.readText() }
                                    .onSuccess {
                                        sourceText = it
                                        feedback = null
                                    }
                                    .onFailure { feedback = it.message ?: "File tidak dapat dibaca." }
                            }
                        },
                    )
                    feedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                } else {
                    SpotifyReviewStep(
                        results = results.orEmpty(),
                        name = name,
                        description = description,
                        onNameChange = { name = it.take(100) },
                        onDescriptionChange = { description = it.take(300) },
                    )
                }
            }
        },
        confirmButton = {
            when {
                mode == PlaylistCreationMode.EMPTY -> Button(
                    onClick = { onCreateEmpty(name.trim()) },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }

                results == null -> Button(
                    onClick = ::findSongs,
                    enabled = parseResult.tracks.isNotEmpty() && !loading,
                ) { Text(if (loading) "Matching…" else "Find songs") }

                else -> Button(
                    onClick = { onImportSpotify(name.trim(), description.trim(), matchedSongs) },
                    enabled = name.isNotBlank() && matchedSongs.isNotEmpty(),
                ) { Text("Create with ${matchedSongs.size} songs") }
            }
        },
        dismissButton = {
            if (mode == PlaylistCreationMode.SPOTIFY && results != null) {
                TextButton(onClick = { results = null }, enabled = !loading) { Text("Back") }
            } else {
                TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun SpotifySourceStep(
    sourceText: String,
    onSourceChange: (String) -> Unit,
    trackCount: Int,
    duplicateCount: Int,
    ignoredCount: Int,
    loading: Boolean,
    completed: Int,
    total: Int,
    onReadClipboard: () -> Unit,
    onChooseFile: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cara menyalin dari Spotify Desktop", fontWeight = FontWeight.SemiBold)
            Text(
                "1. Klik salah satu lagu  2. Tekan Ctrl+A  3. Tekan Ctrl+C  4. Kembali ke Hikalist dan pilih Read clipboard.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick = onReadClipboard, enabled = !loading) {
            Icon(Icons.Default.ContentPaste, null)
            Spacer(Modifier.width(8.dp))
            Text("Read clipboard")
        }
        OutlinedButton(onClick = onChooseFile, enabled = !loading) {
            Icon(Icons.Default.UploadFile, null)
            Spacer(Modifier.width(8.dp))
            Text("Upload .txt")
        }
    }
    OutlinedTextField(
        value = sourceText,
        onValueChange = onSourceChange,
        label = { Text("Spotify track links") },
        placeholder = { Text("Tempel satu atau banyak link lagu Spotify di sini") },
        minLines = 5,
        maxLines = 8,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth(),
    )
    if (trackCount > 0) {
        Text(
            buildString {
                append("$trackCount link valid")
                if (duplicateCount > 0) append(" · $duplicateCount duplicate dihapus")
                if (ignoredCount > 0) append(" · $ignoredCount baris diabaikan")
            },
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (loading) {
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else completed.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Mencocokkan $completed dari $total lagu…")
    }
}

@Composable
private fun SpotifyReviewStep(
    results: List<SpotifyImportResult>,
    name: String,
    description: String,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    val matched = results.count { it.song != null }
    val failed = results.size - matched
    Text("$matched lagu cocok${if (failed > 0) " · $failed perlu dilewati" else ""}", fontWeight = FontWeight.SemiBold)
    LazyColumn(
        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        items(results, key = { it.reference.id }) { result ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val cover = result.song?.thumbnail ?: result.metadata?.coverUrl
                if (!cover.isNullOrBlank()) {
                    AsyncImage(cover, null, Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        result.metadata?.title ?: result.reference.id,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        result.metadata?.artist ?: result.error.orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    if (result.song != null) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    null,
                    tint = if (result.song != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text("Playlist name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        label = { Text("Description") },
        supportingText = { Text("${description.length}/300") },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

private fun readSpotifyClipboard(): String {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    require(clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) { "Clipboard tidak berisi teks." }
    return clipboard.getData(DataFlavor.stringFlavor)?.toString().orEmpty()
}

private fun chooseSpotifyTextFile(): File? {
    val dialog = FileDialog(null as Frame?, "Choose Spotify links file", FileDialog.LOAD).apply {
        file = "*.txt"
        isVisible = true
    }
    val directory = dialog.directory ?: return null
    val filename = dialog.file ?: return null
    return File(directory, filename).takeIf { it.extension.equals("txt", ignoreCase = true) }
}
