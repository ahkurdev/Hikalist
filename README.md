# Hikalist

<p align="center">
  <img src="desktop/app/src/main/resources/hikalist-flower.png" width="180" alt="Hikalist flower artwork">
</p>

<p align="center">
  Pemutar musik desktop Windows yang tenang, lengkap, dan dibuat dengan Kotlin serta Compose Desktop.
</p>

<p align="center">
  <a href="LICENSE"><img alt="License GPL-3.0" src="https://img.shields.io/badge/license-GPL--3.0-eba3b2"></a>
  <img alt="Platform Windows" src="https://img.shields.io/badge/platform-Windows-eba3b2">
  <img alt="Kotlin JVM 21" src="https://img.shields.io/badge/Kotlin-JVM%2021-eba3b2">
  <img alt="Version 1.0.1" src="https://img.shields.io/badge/version-1.0.1-eba3b2">
</p>

Hikalist adalah aplikasi musik desktop mandiri untuk Windows. Aplikasi ini menyediakan pencarian dan streaming musik, playlist lokal maupun kolaboratif, lirik tersinkronisasi, mode offline, integrasi Windows, dan Discord Rich Presence tanpa mengharuskan pengguna memasang VLC atau pemutar eksternal lain.

> Repository ini khusus Hikalist Desktop. Untuk proyek Android, kunjungi [metrolistgroup/metrolist](https://github.com/metrolistgroup/metrolist).

## Fitur utama

### Penemuan musik

- Home yang dipersonalisasi dari pencarian dan kebiasaan dengar lokal.
- Pencarian lagu, album, artis, video, dan playlist.
- Halaman detail album, artis, serta playlist dengan carousel yang dapat digeser.
- Riwayat pemutaran, library, dan Liked Songs.

### Pemutar

- Streaming audio tanpa VLC atau instalasi aplikasi tambahan.
- Play, pause, previous, next, shuffle, repeat one, dan repeat all.
- Antrean berulang: lagu terakhir kembali ke lagu pertama ketika repeat all aktif.
- Seek, progress bar, durasi, volume beserta persentase, dan pilihan audio output.
- Kualitas audio Auto, High, dan Data Saver.
- Equalizer 10-band dengan preset dan gain kustom.
- Sleep timer serta pemulihan antrean/sesi setelah aplikasi dibuka kembali.
- Penanganan kegagalan stream dan perpindahan lagu tanpa meninggalkan status pemutaran lama.

### Lirik

- Lirik biasa, tersinkronisasi per baris, dan format word-timed.
- Resolver multi-provider dengan cache lokal dan fallback.
- Transisi halus, fokus pada baris aktif, serta indikator sebelum lirik pertama dimulai.
- Sanitasi metadata mentah agar tag timing/provider tidak tampil sebagai teks.
- Lirik yang sudah diunduh tetap tersedia saat offline.

### Playlist dan library

- Playlist lokal dengan nama, deskripsi, cover, urutan lagu, dan kolom **Ditambahkan oleh**.
- Liked Songs diperlakukan sebagai playlist, bukan tombol yang langsung memainkan satu lagu.
- Tambah lagu, hapus lagu, susun ulang, dan putar seluruh playlist.
- Edit atau hapus playlist melalui menu konteks.
- Cover playlist tersimpan secara efisien di penyimpanan lokal atau Supabase Storage untuk playlist tersinkronisasi.

### Akun dan kolaborasi

- Fitur lokal dapat digunakan tanpa login.
- Login/register email melalui [web-hikalist-zeta.vercel.app](https://web-hikalist-zeta.vercel.app).
- Profil, username, email, dan avatar pengguna.
- Sinkronisasi playlist otomatis melalui Supabase saat koneksi internet tersedia.
- Realtime update untuk metadata playlist, cover, anggota, urutan, dan lagu.
- Peran owner dan editor: editor dapat menambah, menghapus, serta mengurutkan lagu; owner tetap mengelola detail playlist dan anggota.
- Undangan editor melalui username atau link yang kedaluwarsa setelah dua jam.
- Perubahan offline disimpan lokal dan dikirim kembali ketika koneksi pulih.

### Import Spotify

- Import dari kumpulan link track Spotify tanpa Spotify Web API atau akun Premium.
- Input dapat ditempel langsung atau dibaca dari berkas `.txt`.
- Hikalist mencocokkan judul/artis ke katalog musik, lalu membuat playlist dengan nama dan deskripsi pilihan pengguna.
- Import tidak bergantung pada token Spotify sementara.

### Mode offline

- Download satu lagu atau seluruh playlist.
- Audio, cover, metadata, dan lirik disimpan bersama untuk pemutaran offline.
- Antrean download, progress, pembatalan, retry, dan penghapusan media lokal.
- FFmpeg dibundel bersama aplikasi; pengguna tidak perlu menginstalnya secara terpisah.

### Integrasi desktop

- Custom title bar yang menyatu dengan UI, lengkap dengan minimize, maximize/restore, dan close.
- Resize window serta mode maximized tanpa menutupi taskbar Windows.
- Windows System Media Transport Controls dan dukungan tombol media keyboard.
- Discord Rich Presence otomatis mengikuti lagu, artis, artwork, posisi, pause, pergantian lagu, dan aplikasi ditutup.
- Pemeriksaan pembaruan aplikasi.

## Teknologi

- Kotlin/JVM 21
- Compose Multiplatform Desktop
- JavaFX Media dan FFmpeg
- Ktor + OkHttp
- SQLite + JetBrains Exposed
- Supabase Auth, PostgREST, Realtime, dan Storage
- Koin
- Coil
- JUnit 4

## Struktur repository

```text
Hikalist/
├── desktop/app/        # Aplikasi Compose Desktop, UI, player, sync, dan test
├── innertube/          # Akses katalog dan metadata musik
├── betterlyrics/       # Provider lirik
├── kugou/              # Provider lirik/metadata KuGou
├── lastfm/             # Integrasi metadata Last.fm
├── lrclib/             # Provider LRCLIB
├── paxsenix/           # Provider lirik tambahan
├── shazamkit/          # Pencarian metadata Shazam
├── gradle/             # Version catalog dan Gradle wrapper
├── build.gradle.kts
└── settings.gradle.kts
```

Kode aplikasi utama berada di `desktop/app/src/main/kotlin/com/metrolist/desktop`:

```text
auth/             akun, callback browser, profil, dan avatar
db/               SQLite schema dan repository
discord/          Discord IPC/Rich Presence
lyrics/           resolver, parser, dan cache lirik
offline/          download serta media offline
personalization/  rekomendasi Home lokal
player/           playback, queue, seek, kualitas, equalizer, sleep timer
playlist/         cover dan metadata playlist
spotify/          parser dan import link Spotify
storage/          pengelolaan lokasi data pengguna
sync/             Supabase, realtime, kolaborasi, dan offline-first sync
ui/               Compose Desktop UI
update/           pemeriksaan versi
window/           custom title bar dan resize window
windows/          Windows media session
```

## Persyaratan pengembangan

- Windows 10/11 64-bit.
- JDK 21.
- PowerShell 5.1 atau lebih baru.
- Koneksi internet pada build pertama untuk mengambil dependency Gradle/Maven.

Runtime Java dan FFmpeg sudah dibundel ke installer hasil build. Pengguna akhir tidak perlu memasang JDK, VLC, atau FFmpeg.

## Menjalankan dari source

```powershell
git clone https://github.com/Allan4u/Hikalist.git
cd Hikalist
.\gradlew.bat :desktop:app:run
```

Jika `ffmpeg.exe` belum tersedia di resource setelah clone, jalankan:

```powershell
.\download_ffmpeg.bat
```

## Test

Menjalankan seluruh unit test desktop:

```powershell
.\gradlew.bat :desktop:app:test
```

Test mencakup queue, seek, stream policy, equalizer, session restore, download offline, lirik, Spotify import, sinkronisasi Supabase, kolaborasi, Discord RPC, media controls, update checker, serta geometri window.

Beberapa probe jaringan bersifat opsional dan memerlukan koneksi serta layanan eksternal yang tersedia.

## Build aplikasi dan installer

Membuat folder aplikasi portabel dengan runtime Java:

```powershell
.\gradlew.bat :desktop:app:createDistributable
```

Membuat installer Windows `.exe`:

```powershell
.\gradlew.bat :desktop:app:packageExe
```

Membuat installer Windows `.msi`:

```powershell
.\gradlew.bat :desktop:app:packageMsi
```

Hasilnya berada di:

```text
desktop/app/build/compose/binaries/main/app/
desktop/app/build/compose/binaries/main/exe/
desktop/app/build/compose/binaries/main/msi/
```

Installer Hikalist bersifat per-user, membuat shortcut, mendukung pemilihan lokasi instalasi, dan membawa runtime Java beserta modul yang diperlukan aplikasi.

## Konfigurasi layanan

Build resmi memakai proyek Supabase Hikalist dan web account yang telah dikonfigurasi di source. Untuk deployment sendiri:

1. Ganti project URL dan publishable key pada `HikalistSupabaseClient.kt`.
2. Jalankan migration di `supabase/migrations/` untuk menyiapkan tabel, RLS policy, dan RPC invite.
3. Atur Cloudinary (cloud name + unsigned preset `HikalistCloudinaryConfig.kt`) untuk upload avatar dan cover playlist, maksimal 10 MB.
4. Atur `HIKALIST_ACCOUNT_URL` jika web account tidak berada di URL default.
5. Ganti Discord Application ID bila memakai aplikasi Discord sendiri.

Client desktop hanya boleh membawa Supabase **publishable/anon key**. Jangan pernah memasukkan `service_role` key, password database, SMTP credential, access token pribadi, cookie, atau client secret ke repository.

## Data dan privasi

- Database, playlist lokal, history, preferensi, cache, dan download disimpan di direktori data pengguna Windows.
- Akun hanya diperlukan untuk profil, sinkronisasi, dan kolaborasi.
- Supabase RLS membatasi data cloud sesuai akun serta keanggotaan playlist.
- Hikalist menghubungi layanan katalog, artwork, lirik, dan metadata hanya untuk menyediakan fitur aplikasi.

## Troubleshooting

- **`javax/naming/NamingException` atau `Failed to launch JVM`:** gunakan installer 1.0.1 atau yang lebih baru; runtime tersebut sudah menyertakan modul `java.naming`.
- **Audio tidak keluar:** periksa output audio di Settings, lalu coba kualitas Auto.
- **Seek berhenti:** coba ulang setelah koneksi stabil; player akan me-resolve stream bila URL lama kedaluwarsa.
- **Lirik kosong:** buka ulang panel lirik agar resolver mencoba cache dan provider fallback.
- **Discord tidak berubah:** pastikan Discord Desktop berjalan dan Rich Presence aktif di Settings.
- **Sinkronisasi tertunda:** perubahan tetap berada di antrean lokal dan akan dikirim saat internet serta sesi login tersedia.
- **Build gagal menemukan Java:** pastikan `java -version` menunjukkan JDK 21.

## Pembuat

Hikalist Desktop dikembangkan dan dipelihara oleh [Allan4u](https://github.com/Allan4u).

## Atribusi

Hikalist Desktop dikembangkan dari pekerjaan dan layanan JVM yang berasal dari ekosistem [Metrolist](https://github.com/metrolistgroup/metrolist). Metrolist tetap merupakan proyek Android terpisah; repository ini tidak menyatakan dukungan resmi dari tim Metrolist.

FFmpeg untuk Windows disediakan oleh [Gyan Doshi](https://www.gyan.dev/ffmpeg/builds/). Library pihak ketiga lain tercantum dalam Gradle version catalog dan mengikuti lisensinya masing-masing.

## Lisensi

Hikalist didistribusikan di bawah [GNU General Public License v3.0](LICENSE). Distribusi ulang dan karya turunan harus mematuhi GPL-3.0 serta lisensi dependency yang digunakan.

## Catatan penggunaan

Hikalist adalah client tidak resmi. Pengguna bertanggung jawab memastikan penggunaan media, metadata, dan layanan pihak ketiga mematuhi ketentuan layanan serta hukum yang berlaku di wilayah masing-masing.
