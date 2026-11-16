Bahasa Indonesia version: [readme.id.md](readme.id.md)

# Hikalist

<p align="center">
  <img src="desktop/app/src/main/resources/hikalist-flower.png" width="180" alt="Hikalist flower artwork">
</p>

<p align="center">
  A calm, complete Windows desktop music player, built with Kotlin and Compose Desktop.
</p>

<p align="center">
  <a href="LICENSE"><img alt="License GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-eba3b2"></a>
  <img alt="Platform Windows" src="https://img.shields.io/badge/platform-Windows-eba3b2">
  <img alt="Kotlin JVM 21" src="https://img.shields.io/badge/Kotlin-JVM%2021-eba3b2">
  <img alt="Version 1.0.5" src="https://img.shields.io/badge/version-1.0.5-eba3b2">
</p>

Hikalist is a standalone desktop music app for Windows. It provides music search and streaming, local and collaborative playlists, synced lyrics, offline mode, Windows integration, and Discord Rich Presence without requiring users to install VLC or any other external player.

> This repository is for Hikalist Desktop only. For the Android project, visit [metrolistgroup/metrolist](https://github.com/metrolistgroup/metrolist).

## Key features

### Music discovery

- Personalised Home based on local search and listening habits.
- Search for songs, albums, artists, videos, and playlists.
- Album, artist, and playlist detail pages with swipeable carousels.
- Play history, library, and Liked Songs.

### Player

- Audio streaming without VLC or installing additional apps.
- Play, pause, previous, next, shuffle, repeat one, and repeat all.
- Looping queue: the last song returns to the first song when repeat all is active.
- Seek, progress bar, duration, volume with percentage, and audio output selection.
- Audio quality Auto, High, and Data Saver.
- 10-band equalizer with presets and custom gain.
- Sleep timer and queue/session restore after the app is reopened.
- Stream failure handling and track switching without leaving stale playback state.

### Lyrics

- Plain, line-synced, and word-timed lyrics.
- Multi-provider resolver with local cache and fallback.
- Smooth transitions, focus on the active line, and an indicator before the first lyric starts.
- Raw metadata sanitisation so timing/provider tags don't show as text.
- Downloaded lyrics remain available offline.

### Playlists and library

- Local playlists with name, description, cover, song order, and an **Added by** column.
- Liked Songs is treated as a playlist, not a button that plays a single song directly.
- Add songs, remove songs, reorder, and play the whole playlist.
- Edit or delete playlists via the context menu.
- Playlist covers are stored efficiently in local storage or Supabase Storage for synced playlists.

### Accounts and collaboration

- Local features can be used without logging in.
- Email login/register via [hikalist.ahkur.my.id](https://hikalist.ahkur.my.id).
- User profile, username, email, and avatar.
- Automatic playlist sync via Supabase when an internet connection is available.
- Realtime updates for playlist metadata, covers, members, order, and songs.
- Owner and editor roles: editors can add, remove, and reorder songs; the owner still manages playlist details and members.
- Editor invitations via username or a link that expires after two hours.
- Offline changes are stored locally and sent again when the connection recovers.

### Spotify import

- Import from a set of Spotify track links without the Spotify Web API or a Premium account.
- Input can be pasted directly or read from a `.txt` file.
- Hikalist matches title/artist to the music catalogue, then creates a playlist with the user's chosen name and description.
- Import does not rely on temporary Spotify tokens.

### Offline mode

- Download a single song or an entire playlist.
- Audio, cover, metadata, and lyrics are stored together for offline playback.
- Download queue, progress, cancellation, retry, and local media deletion.
- FFmpeg is bundled with the app; users don't need to install it separately.

### Desktop integration

- Custom title bar that blends with the UI, complete with minimize, maximize/restore, and close.
- Window resize and maximized mode without covering the Windows taskbar.
- Windows System Media Transport Controls and keyboard media button support.
- Discord Rich Presence automatically follows the song, artist, artwork, position, pause, track changes, and when the app closes.
- App update checks.

## Technologies

- Kotlin/JVM 21
- Compose Multiplatform Desktop
- JavaFX Media and FFmpeg
- Ktor + OkHttp
- SQLite + JetBrains Exposed
- Supabase Auth, PostgREST, Realtime, and Storage
- Koin
- Coil
- JUnit 4

## Repository structure

```text
Hikalist/
├── desktop/app/        # Compose Desktop app, UI, player, sync, and tests
├── innertube/          # Music catalogue and metadata access
├── betterlyrics/       # Lyrics provider
├── kugou/              # KuGou lyrics/metadata provider
├── lastfm/             # Last.fm metadata integration
├── lrclib/             # LRCLIB provider
├── paxsenix/           # Additional lyrics provider
├── shazamkit/          # Shazam metadata search
├── gradle/             # Version catalog and Gradle wrapper
├── build.gradle.kts
└── settings.gradle.kts
```

The main app code is in `desktop/app/src/main/kotlin/com/metrolist/desktop`:

```text
auth/             account, browser callback, profile, and avatar
db/               SQLite schema and repository
discord/          Discord IPC/Rich Presence
lyrics/           lyric resolver, parser, and cache
offline/          downloads and offline media
personalization/  local Home recommendations
player/           playback, queue, seek, quality, equalizer, sleep timer
playlist/         playlist cover and metadata
spotify/          Spotify link parser and import
storage/          user data location management
sync/             Supabase, realtime, collaboration, and offline-first sync
ui/               Compose Desktop UI
update/           version checks
window/           custom title bar and window resize
windows/          Windows media session
```

## Development requirements

- Windows 10/11 64-bit.
- JDK 21.
- PowerShell 5.1 or newer.
- Internet connection on the first build to fetch Gradle/Maven dependencies.

The Java runtime and FFmpeg are already bundled into the built installer. End users don't need to install the JDK, VLC, or FFmpeg.

## Running from source

```powershell
git clone https://github.com/Allan4u/Hikalist.git
cd Hikalist
.\gradlew.bat :desktop:app:run
```

If `ffmpeg.exe` is not yet available in the resources after cloning, run:

```powershell
.\download_ffmpeg.bat
```

## Tests

Run all desktop unit tests:

```powershell
.\gradlew.bat :desktop:app:test
```

Tests cover queue, seek, stream policy, equalizer, session restore, offline downloads, lyrics, Spotify import, Supabase sync, collaboration, Discord RPC, media controls, update checker, and window geometry.

Some network probes are optional and require an available connection and external services.

## Building the app and installer

Create a portable app folder with the Java runtime:

```powershell
.\gradlew.bat :desktop:app:createDistributable
```

Create a Windows `.exe` installer:

```powershell
.\gradlew.bat :desktop:app:packageExe
```

Create a Windows `.msi` installer:

```powershell
.\gradlew.bat :desktop:app:packageMsi
```

The results are in:

```text
desktop/app/build/compose/binaries/main/app/
desktop/app/build/compose/binaries/main/exe/
desktop/app/build/compose/binaries/main/msi/
```

The Hikalist installer is per-user, creates a shortcut, supports choosing the install location, and bundles the Java runtime with the modules the app needs.

## Service configuration

The official build uses the configured Hikalist Supabase project and web account in the source. For your own deployment:

1. Change the project URL and publishable key in `HikalistSupabaseClient.kt`.
2. Run the migrations in `supabase/migrations/` to set up the tables, RLS policies, and invite RPC.
3. Set up Cloudinary (cloud name + unsigned preset `HikalistCloudinaryConfig.kt`) for avatar and playlist cover uploads, max 10 MB.
4. Set `HIKALIST_ACCOUNT_URL` if the web account is not at the default URL.
5. Change the Discord Application ID if using your own Discord application.

The desktop client must only carry the Supabase **publishable/anon key**. Never put a `service_role` key, database password, SMTP credential, personal access token, cookie, or client secret in the repository.

## Data and privacy

- Database, local playlists, history, preferences, cache, and downloads are stored in the Windows user data directory.
- An account is only needed for profile, sync, and collaboration.
- Supabase RLS restricts cloud data by account and playlist membership.
- Hikalist contacts catalogue, artwork, lyrics, and metadata services only to provide the app's features.

## Troubleshooting

- **`javax/naming/NamingException` or `Failed to launch JVM`:** use installer 1.0.2 or newer; that runtime already includes the `java.naming` module.
- **No audio output:** check the audio output in Settings, then try Auto quality.
- **Seek stops:** try again after the connection stabilises; the player will re-resolve the stream when the old URL expires.
- **Empty lyrics:** reopen the lyrics panel so the resolver tries the cache and provider fallback.
- **Discord doesn't change:** make sure Discord Desktop is running and Rich Presence is enabled in Settings.
- **Sync delayed:** changes stay in the local queue and will be sent when internet and a login session are available.
- **Build can't find Java:** make sure `java -version` shows JDK 21.

## Author

Hikalist Desktop is developed and maintained by [Allan4u](https://github.com/Allan4u).

## Attribution

Hikalist Desktop is developed from JVM work and services originating from the [Metrolist](https://github.com/metrolistgroup/metrolist) ecosystem. Metrolist remains a separate Android project; this repository does not claim official support from the Metrolist team.

FFmpeg for Windows is provided by [Gyan Doshi](https://www.gyan.dev/ffmpeg/builds/). Other third-party libraries are listed in the Gradle version catalog and follow their respective licenses.

## License

Hikalist is distributed under the [GNU General Public License v3.0](LICENSE). Redistribution and derivative works must comply with GPL-3.0 and the licenses of the dependencies used.

## Usage notes

Hikalist is an unofficial client. Users are responsible for ensuring their use of media, metadata, and third-party services complies with the terms of service and laws applicable in their region.
