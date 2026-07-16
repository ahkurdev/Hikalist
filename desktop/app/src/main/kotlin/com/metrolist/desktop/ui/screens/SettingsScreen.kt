package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.desktop.preferences.AppSettings
import com.metrolist.desktop.auth.DesktopAccountState
import com.metrolist.desktop.auth.DesktopAccountStatus
import com.metrolist.desktop.preferences.AudioQualityPreference
import com.metrolist.desktop.preferences.RepeatPreference
import com.metrolist.desktop.preferences.ThemePreference
import com.metrolist.desktop.player.AudioOutputDevice
import com.metrolist.desktop.player.SleepTimerOption
import com.metrolist.desktop.player.SleepTimerState
import com.metrolist.desktop.storage.StorageManager
import com.metrolist.desktop.storage.StorageSnapshot
import com.metrolist.desktop.sync.PlaylistSyncStatus
import com.metrolist.desktop.sync.SyncConnectionState
import com.metrolist.desktop.update.UpdateChecker
import com.metrolist.desktop.update.UpdateState
import java.awt.Desktop
import java.time.Instant
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    settings: AppSettings,
    accountState: StateFlow<DesktopAccountState>,
    syncStatus: StateFlow<PlaylistSyncStatus>,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateProfile: (String, File?) -> Unit,
    onSyncNow: () -> Unit,
    onVolumePreview: (Int) -> Unit,
    onOpenEqualizer: () -> Unit,
    audioOutputDevices: List<AudioOutputDevice>,
    onAudioOutputSelected: (String) -> Unit,
    storageManager: StorageManager,
    sleepTimerState: StateFlow<SleepTimerState>,
    onSleepTimerSelected: (SleepTimerOption) -> Unit,
    updateChecker: UpdateChecker,
) {
    val theme by settings.theme.collectAsState()
    val audioQuality by settings.audioQuality.collectAsState()
    val persistedVolume by settings.volumePercent.collectAsState()
    val showLyrics by settings.showLyricsByDefault.collectAsState()
    val discordRichPresence by settings.discordRichPresence.collectAsState()
    val repeat by settings.repeat.collectAsState()
    val equalizerEnabled by settings.equalizerEnabled.collectAsState()
    val equalizerPreset by settings.equalizerPreset.collectAsState()
    val selectedAudioOutputId by settings.audioOutputId.collectAsState()
    val account by accountState.collectAsState()
    val playlistSync by syncStatus.collectAsState()
    val sleepTimer by sleepTimerState.collectAsState()
    val updateState by updateChecker.state.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var volume by remember { mutableFloatStateOf(persistedVolume.toFloat()) }
    var volumeAdjusting by remember { mutableStateOf(false) }
    var editProfileVisible by remember { mutableStateOf(false) }
    var audioOutputMenuVisible by remember { mutableStateOf(false) }
    var storageSnapshot by remember { mutableStateOf<StorageSnapshot?>(null) }
    var confirmDeleteDownloads by remember { mutableStateOf(false) }

    suspend fun refreshStorage() {
        storageSnapshot = withContext(Dispatchers.IO) { storageManager.snapshot() }
    }

    LaunchedEffect(storageManager) { refreshStorage() }

    LaunchedEffect(persistedVolume) {
        if (!volumeAdjusting) volume = persistedVolume.toFloat()
    }

    Column(
        Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 34.dp, vertical = 28.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Text(
            "Make Hikalist feel and sound right for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        SettingsCard("Appearance", "Choose how Hikalist follows your desktop.") {
            ChoiceRow(
                choices = ThemePreference.entries,
                selected = theme,
                label = {
                    when (it) {
                        ThemePreference.SYSTEM -> "System"
                        ThemePreference.DARK -> "Dark"
                        ThemePreference.LIGHT -> "Light"
                    }
                },
                onSelect = settings::setTheme,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Audio quality", audioQuality.description()) {
            ChoiceRow(
                choices = AudioQualityPreference.entries,
                selected = audioQuality,
                label = {
                    when (it) {
                        AudioQualityPreference.AUTO -> "Auto"
                        AudioQualityPreference.HIGH -> "High"
                        AudioQualityPreference.DATA_SAVER -> "Data Saver"
                    }
                },
                onSelect = settings::setAudioQuality,
            )
            Text(
                "The new quality is used from the next stream that Hikalist opens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard(
            "Equalizer",
            if (equalizerEnabled) {
                "Active · ${equalizerPreset.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)}"
            } else {
                "Off · Original sound"
            },
        ) {
            Text(
                "Shape ten frequency bands in realtime. The current song keeps playing while you adjust it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onOpenEqualizer) { Text("Open equalizer") }
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Playback", "Your playback defaults are saved on this computer.") {
            SettingLabel(
                "Audio output",
                audioOutputDevices.firstOrNull { it.id == selectedAudioOutputId }?.displayName ?: "System default",
            )
            androidx.compose.foundation.layout.Box {
                OutlinedButton(onClick = { audioOutputMenuVisible = true }) { Text("Choose output device") }
                DropdownMenu(
                    expanded = audioOutputMenuVisible,
                    onDismissRequest = { audioOutputMenuVisible = false },
                ) {
                    audioOutputDevices.forEach { device ->
                        DropdownMenuItem(
                            text = { Text(device.displayName) },
                            onClick = {
                                audioOutputMenuVisible = false
                                onAudioOutputSelected(device.id)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SettingLabel("Volume", "${volume.roundToInt()}%")
            Slider(
                value = volume,
                onValueChange = {
                    volumeAdjusting = true
                    volume = it
                    onVolumePreview(it.roundToInt())
                },
                onValueChangeFinished = {
                    settings.setVolumePercent(volume.roundToInt())
                    volumeAdjusting = false
                },
                valueRange = 0f..100f,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            SettingLabel("Repeat when Hikalist starts", repeat.label())
            ChoiceRow(
                choices = RepeatPreference.entries,
                selected = repeat,
                label = RepeatPreference::label,
                onSelect = settings::setRepeat,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show lyrics by default", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Open the lyric panel automatically for each song.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = showLyrics, onCheckedChange = settings::setShowLyricsByDefault)
            }
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Sleep timer", sleepTimer.description()) {
            ChoiceRow(
                choices = SleepTimerOption.entries,
                selected = sleepTimer.option,
                label = SleepTimerOption::label,
                onSelect = onSleepTimerSelected,
            )
            Text(
                "Playback stops automatically. Your queue and current position remain saved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Storage", "Manage offline music and disposable cache separately.") {
            SettingLabel("Downloaded music", humanReadableBytes(storageSnapshot?.downloadBytes ?: 0L))
            SettingLabel("Cache", humanReadableBytes(storageSnapshot?.cacheBytes ?: 0L))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { storageManager.openDownloadDirectory() }) { Text("Open folder") }
                OutlinedButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { storageManager.clearCache() }
                        refreshStorage()
                    }
                }) { Text("Clear cache") }
                TextButton(onClick = { confirmDeleteDownloads = true }) { Text("Delete downloads") }
            }
            Text(
                "Clearing cache never removes downloads, playlist covers, your database, or account data.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard(
            "Discord Rich Presence",
            if (discordRichPresence) "Automatically following your playback." else "Hidden from Discord.",
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Show what I am listening to", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Shows the title, artist, cover, playback timer, and YouTube Music link while Discord Desktop is open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = discordRichPresence,
                    onCheckedChange = settings::setDiscordRichPresence,
                )
            }
            Text(
                "Uses a private connection on this computer. No Discord server or separate Hikalist authorization is needed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Hikalist account", account.description()) {
            when (account.status) {
                DesktopAccountStatus.INITIALIZING -> Text("Loading your saved session…")
                DesktopAccountStatus.WAITING_FOR_BROWSER -> Text("Continue in Brave to finish securely.")
                DesktopAccountStatus.SIGNED_IN -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        account.avatarUrl?.let { avatarUrl ->
                            AsyncImage(
                                model = avatarUrl,
                                contentDescription = "Hikalist account avatar",
                                modifier = Modifier.size(52.dp).clip(CircleShape),
                            )
                        }
                        Column {
                            account.username?.let { SettingLabel("Username", it) }
                            account.email?.let { SettingLabel("Email", it) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { editProfileVisible = true }) { Text("Edit profile") }
                        OutlinedButton(onClick = onSignOut) { Text("Sign out") }
                    }
                }
                DesktopAccountStatus.SIGNED_OUT,
                DesktopAccountStatus.ERROR,
                -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onLogin) { Text("Log in") }
                    OutlinedButton(onClick = onRegister) { Text("Create account") }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Cloud sync", playlistSync.description()) {
            SettingLabel("Status", playlistSync.label())
            if (playlistSync.pendingOperations > 0) {
                SettingLabel("Waiting to upload", playlistSync.pendingOperations.toString())
            }
            playlistSync.lastSyncedAtEpochSeconds?.let { timestamp ->
                SettingLabel("Last synced", formatSyncTime(timestamp))
            }
            Text(
                "Playlists retry automatically when the internet connection returns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSyncNow,
                enabled = account.status == DesktopAccountStatus.SIGNED_IN &&
                    playlistSync.state != SyncConnectionState.SYNCING,
            ) {
                Text(if (playlistSync.state == SyncConnectionState.SYNCING) "Syncing…" else "Sync now")
            }
        }
        Spacer(Modifier.height(16.dp))

        SettingsCard("Hikalist updates", updateState.description()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { scope.launch { updateChecker.check() } },
                    enabled = updateState != UpdateState.Checking,
                ) { Text(if (updateState == UpdateState.Checking) "Checking..." else "Check for updates") }
                (updateState as? UpdateState.Available)?.let { available ->
                    OutlinedButton(onClick = { openWebPage(available.pageUrl) }) { Text("View release") }
                }
            }
            Text(
                "Hikalist only checks the official GitHub Releases page. Installation always remains under your control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Stored locally", style = MaterialTheme.typography.titleMedium)
                Text(
                    "These preferences stay on this computer and are not uploaded to your Hikalist account.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    if (editProfileVisible) {
        EditProfileDialog(
            account = account,
            onDismiss = { editProfileVisible = false },
            onSave = { username, avatar ->
                onUpdateProfile(username, avatar)
                editProfileVisible = false
            },
        )
    }

    if (confirmDeleteDownloads) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDownloads = false },
            title = { Text("Delete all downloads?") },
            text = { Text("Offline audio and its saved offline artwork will be removed. Your playlists stay intact.") },
            confirmButton = {
                Button(onClick = {
                    confirmDeleteDownloads = false
                    scope.launch {
                        withContext(Dispatchers.IO) { storageManager.clearDownloads() }
                        refreshStorage()
                    }
                }) { Text("Delete all") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteDownloads = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun EditProfileDialog(
    account: DesktopAccountState,
    onDismiss: () -> Unit,
    onSave: (String, File?) -> Unit,
) {
    var username by remember(account.userId) { mutableStateOf(account.username.orEmpty()) }
    var avatarFile by remember(account.userId) { mutableStateOf<File?>(null) }
    val avatarModel = avatarFile?.absolutePath ?: account.avatarUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Hikalist profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                avatarModel?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = "Profile avatar preview",
                        modifier = Modifier.size(82.dp).clip(CircleShape),
                    )
                }
                OutlinedButton(onClick = { chooseAvatar()?.let { avatarFile = it } }) {
                    Text("Choose avatar")
                }
                Text(
                    "JPG, PNG, or WebP up to 2 MB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.take(40) },
                    label = { Text("Username") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(username.trim(), avatarFile) }, enabled = username.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun chooseAvatar(): File? {
    val dialog = FileDialog(null as Frame?, "Choose Hikalist avatar", FileDialog.LOAD).apply {
        filenameFilter = java.io.FilenameFilter { _, name ->
            name.substringAfterLast('.', "").lowercase() in setOf("jpg", "jpeg", "png", "webp")
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
private fun SettingsCard(
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        modifier = Modifier.widthIn(max = 820.dp).fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(horizontal = 22.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            content()
        }
    }
}

@Composable
private fun <T> ChoiceRow(
    choices: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = choice == selected,
                onClick = { onSelect(choice) },
                label = { Text(label(choice)) },
            )
        }
    }
}

@Composable
private fun SettingLabel(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

private fun AudioQualityPreference.description(): String = when (this) {
    AudioQualityPreference.AUTO -> "Balanced quality around 128 kbps. Recommended for everyday listening."
    AudioQualityPreference.HIGH -> "Uses the highest available bitrate for the selected audio codec."
    AudioQualityPreference.DATA_SAVER -> "Uses the lowest valid bitrate to reduce data usage."
}

private fun RepeatPreference.label(): String = when (this) {
    RepeatPreference.OFF -> "Off"
    RepeatPreference.ALL -> "All"
    RepeatPreference.ONE -> "One"
}

private fun SleepTimerOption.label(): String = when (this) {
    SleepTimerOption.OFF -> "Off"
    SleepTimerOption.MINUTES_15 -> "15 min"
    SleepTimerOption.MINUTES_30 -> "30 min"
    SleepTimerOption.MINUTES_45 -> "45 min"
    SleepTimerOption.MINUTES_60 -> "60 min"
    SleepTimerOption.END_OF_TRACK -> "End of track"
}

private fun SleepTimerState.description(): String = when (option) {
    SleepTimerOption.OFF -> "No timer is active."
    SleepTimerOption.END_OF_TRACK -> "Hikalist will stop when the current song ends."
    else -> {
        val remaining = remainingSeconds ?: 0L
        "Stopping in ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')}"
    }
}

private fun UpdateState.description(): String = when (this) {
    UpdateState.Idle -> "Version ${com.metrolist.desktop.update.AppVersion.CURRENT.value}"
    UpdateState.Checking -> "Checking the official Hikalist release page..."
    UpdateState.UpToDate -> "Hikalist is up to date."
    is UpdateState.Available -> "Version ${version.value} is available."
    is UpdateState.Failed -> message
}

private fun humanReadableBytes(bytes: Long): String = when {
    bytes < 1_024L -> "$bytes B"
    bytes < 1_048_576L -> "%.1f KB".format(bytes / 1_024.0)
    bytes < 1_073_741_824L -> "%.1f MB".format(bytes / 1_048_576.0)
    else -> "%.2f GB".format(bytes / 1_073_741_824.0)
}

private fun openWebPage(url: String): Boolean = runCatching {
    if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) return@runCatching false
    Desktop.getDesktop().browse(java.net.URI.create(url))
    true
}.getOrDefault(false)

private fun PlaylistSyncStatus.label(): String = when (state) {
    SyncConnectionState.STARTING -> "Starting"
    SyncConnectionState.SYNCING -> "Syncing"
    SyncConnectionState.SYNCED -> "Up to date"
    SyncConnectionState.OFFLINE -> "Offline"
    SyncConnectionState.AUTH_REQUIRED -> "Sign in required"
    SyncConnectionState.ERROR -> "Needs attention"
}

private fun PlaylistSyncStatus.description(): String = message ?: when (state) {
    SyncConnectionState.STARTING -> "Preparing secure playlist sync."
    SyncConnectionState.SYNCING -> "Sending and receiving playlist changes."
    SyncConnectionState.SYNCED -> "Your playlist changes are safely stored in Hikalist Cloud."
    SyncConnectionState.OFFLINE -> "Changes stay safely queued on this computer."
    SyncConnectionState.AUTH_REQUIRED -> "Sign in to sync playlists across your devices."
    SyncConnectionState.ERROR -> "Hikalist will retry without losing local changes."
}

private fun DesktopAccountState.description(): String = message ?: when (status) {
    DesktopAccountStatus.INITIALIZING -> "Checking the account saved on this computer."
    DesktopAccountStatus.SIGNED_OUT -> "Log in to sync playlists across your Hikalist devices."
    DesktopAccountStatus.WAITING_FOR_BROWSER -> "The secure account page is open in your browser."
    DesktopAccountStatus.SIGNED_IN -> "Signed in${username?.let { " as $it" } ?: email?.let { " as $it" }.orEmpty()}."
    DesktopAccountStatus.ERROR -> "Account connection needs attention. You can safely try again."
}

private fun formatSyncTime(epochSeconds: Long): String = DateTimeFormatter
    .ofPattern("MMM d, HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochSecond(epochSeconds))
