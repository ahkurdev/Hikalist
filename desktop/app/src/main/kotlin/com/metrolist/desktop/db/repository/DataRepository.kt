package com.metrolist.desktop.db.repository

import com.metrolist.desktop.db.DatabaseFactory
import com.metrolist.desktop.db.schema.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class DataRepository {

    init { DatabaseFactory.init() }

    // === SONGS ===

    fun getAllSongs(): List<SongRow> = transaction {
        Songs.selectAll().map { it.toSongWithArtists() }
    }

    fun getSongById(id: String): SongRow? = transaction {
        Songs.selectAll().where { Songs.id eq id }.singleOrNull()?.toSongWithArtists()
    }

    fun searchSongs(query: String): List<SongRow> = transaction {
        Songs.selectAll()
            .where { Songs.title.lowerCase() like "%${query.lowercase()}%" }
            .orderBy(Songs.title)
            .map { it.toSongWithArtists() }
    }

    fun insertSong(song: SongRow) = transaction {
        Songs.insert {
            it[id] = song.id
            it[title] = song.title
            it[duration] = song.duration
            it[thumbnailUrl] = song.thumbnailUrl
            it[albumId] = song.albumId
            it[albumName] = song.albumName
            it[explicit] = if (song.explicit) 1 else 0
            it[year] = song.year
            it[liked] = if (song.liked) 1 else 0
            it[likedDate] = song.likedDate
            it[totalPlayTime] = song.totalPlayTime
            it[inLibrary] = song.inLibrary
            it[isLocal] = if (song.isLocal) 1 else 0
            it[isDownloaded] = if (song.isDownloaded) 1 else 0
            it[isVideo] = if (song.isVideo) 1 else 0
            it[playbackPosition] = song.playbackPosition
        }
    }

    fun saveSong(song: SongRow) = transaction {
        val exists = Songs.selectAll().where { Songs.id eq song.id }.any()
        if (exists) {
            Songs.update({ Songs.id eq song.id }) {
                it[title] = song.title
                it[duration] = song.duration
                it[thumbnailUrl] = song.thumbnailUrl
                it[albumId] = song.albumId
                it[albumName] = song.albumName ?: song.album
                it[explicit] = if (song.explicit) 1 else 0
                it[year] = song.year
            }
        } else {
            Songs.insert {
                it[id] = song.id
                it[title] = song.title
                it[duration] = song.duration
                it[thumbnailUrl] = song.thumbnailUrl
                it[albumId] = song.albumId
                it[albumName] = song.albumName ?: song.album
                it[explicit] = if (song.explicit) 1 else 0
                it[year] = song.year
                it[liked] = if (song.liked) 1 else 0
                it[likedDate] = song.likedDate
                it[totalPlayTime] = song.totalPlayTime
                it[inLibrary] = song.inLibrary
                it[isLocal] = if (song.isLocal) 1 else 0
                it[isDownloaded] = if (song.isDownloaded) 1 else 0
                it[isVideo] = if (song.isVideo) 1 else 0
                it[playbackPosition] = song.playbackPosition
            }
        }

        SongArtistMaps.deleteWhere { SongArtistMaps.songId eq song.id }
        song.artists.orEmpty().filter(String::isNotBlank).distinct().forEachIndexed { index, name ->
            val artistId = "local:${name.lowercase().hashCode()}"
            if (!Artists.selectAll().where { Artists.id eq artistId }.any()) {
                Artists.insert {
                    it[id] = artistId
                    it[this.name] = name
                    it[isLocal] = 1
                }
            }
            SongArtistMaps.insert {
                it[songId] = song.id
                it[this.artistId] = artistId
                it[position] = index
            }
        }
    }

    fun deleteSong(id: String) = transaction {
        Songs.deleteWhere { Songs.id eq id }
    }

    fun getLikedSongs(): List<SongRow> = transaction {
        Songs.selectAll()
            .where { Songs.liked eq 1 }
            .orderBy(Songs.likedDate, SortOrder.DESC)
            .map { it.toSongWithArtists() }
    }

    fun setSongLiked(songId: String, liked: Boolean) = transaction {
        Songs.update({ Songs.id eq songId }) {
            it[this.liked] = if (liked) 1 else 0
            it[likedDate] = if (liked) (System.currentTimeMillis() / 1000).toInt() else null
        }
    }

    fun getDownloadedSongs(): List<SongRow> = transaction {
        Songs.selectAll()
            .where { Songs.isDownloaded eq 1 }
            .map { it.toSong() }
    }

    // === PLAYLISTS ===

    fun getAllPlaylists(): List<PlaylistRow> = transaction {
        Playlists.selectAll()
            .orderBy(Playlists.createdAt, SortOrder.DESC)
            .map { it.toPlaylist() }
    }

    fun getSyncedPlaylistIds(): Set<String> = transaction {
        Playlists.selectAll()
            .where { Playlists.isLocal eq 0 }
            .mapTo(mutableSetOf()) { it[Playlists.id] }
    }

    fun getPlaylistById(id: String): PlaylistRow? = transaction {
        Playlists.selectAll().where { Playlists.id eq id }.singleOrNull()?.toPlaylist()
    }

    fun createPlaylist(name: String): PlaylistRow = transaction {
        val id = "LP${UUID.randomUUID().toString().replace("-", "").take(12)}"
        Playlists.insert {
            it[this.id] = id
            it[this.name] = name
            it[isEditable] = 1
            it[isLocal] = 1
            it[createdAt] = (System.currentTimeMillis() / 1000).toInt()
        }
        Playlists.selectAll().where { Playlists.id eq id }.single().toPlaylist()
    }

    fun deletePlaylist(id: String) = transaction {
        PlaylistSongMaps.deleteWhere { PlaylistSongMaps.playlistId eq id }
        Playlists.deleteWhere { Playlists.id eq id }
    }

    fun renamePlaylist(id: String, name: String) = transaction {
        Playlists.update({ Playlists.id eq id }) { it[this.name] = name.trim() }
    }

    fun updatePlaylistCover(id: String, coverPath: String?) = transaction {
        Playlists.update({ Playlists.id eq id }) { it[thumbnailUrl] = coverPath }
    }

    fun upsertSyncedPlaylist(id: String, name: String, coverUrl: String?) = transaction {
        val exists = Playlists.selectAll().where { Playlists.id eq id }.any()
        if (exists) {
            Playlists.update({ Playlists.id eq id }) {
                it[this.name] = name.trim()
                it[thumbnailUrl] = coverUrl
                it[isEditable] = 1
            }
        } else {
            Playlists.insert {
                it[this.id] = id
                it[this.name] = name.trim()
                it[thumbnailUrl] = coverUrl
                it[isEditable] = 1
                it[isLocal] = 0
                it[createdAt] = (System.currentTimeMillis() / 1_000L).toInt()
            }
        }
    }

    // === PLAYLIST SONGS ===

    fun getPlaylistSongs(playlistId: String): List<SongRow> = transaction {
        (PlaylistSongMaps innerJoin Songs)
            .selectAll()
            .where { PlaylistSongMaps.playlistId eq playlistId }
            .orderBy(PlaylistSongMaps.position)
            .map { it.toSongWithArtists() }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) = transaction {
        val alreadyAdded = PlaylistSongMaps.selectAll().where {
            (PlaylistSongMaps.playlistId eq playlistId) and (PlaylistSongMaps.songId eq songId)
        }.any()
        if (alreadyAdded) return@transaction
        val maxPos = PlaylistSongMaps.selectAll()
            .where { PlaylistSongMaps.playlistId eq playlistId }
            .maxOfOrNull { it[PlaylistSongMaps.position] } ?: -1
        PlaylistSongMaps.insert {
            it[this.playlistId] = playlistId
            it[this.songId] = songId
            it[position] = maxPos + 1
        }
    }

    fun addSongsToPlaylist(playlistId: String, songs: List<SongRow>) {
        songs.distinctBy(SongRow::id).forEach { song ->
            saveSong(song)
            addSongToPlaylist(playlistId, song.id)
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) = transaction {
        PlaylistSongMaps.deleteWhere {
            (PlaylistSongMaps.playlistId eq playlistId) and (PlaylistSongMaps.songId eq songId)
        }
    }

    fun movePlaylistSong(playlistId: String, fromIndex: Int, toIndex: Int) = transaction {
        val entries = PlaylistSongMaps.selectAll()
            .where { PlaylistSongMaps.playlistId eq playlistId }
            .orderBy(PlaylistSongMaps.position)
            .map { it[PlaylistSongMaps.id] }
            .toMutableList()
        if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) return@transaction
        val moved = entries.removeAt(fromIndex)
        entries.add(toIndex, moved)
        entries.forEachIndexed { index, id ->
            PlaylistSongMaps.update({ PlaylistSongMaps.id eq id }) { it[position] = index }
        }
    }

    fun setPlaylistSongPosition(playlistId: String, songId: String, targetPosition: Int) = transaction {
        val entry = PlaylistSongMaps.selectAll().where {
            (PlaylistSongMaps.playlistId eq playlistId) and (PlaylistSongMaps.songId eq songId)
        }.singleOrNull() ?: return@transaction
        PlaylistSongMaps.update({ PlaylistSongMaps.id eq entry[PlaylistSongMaps.id] }) {
            it[position] = targetPosition.coerceAtLeast(0)
        }
        val orderedIds = PlaylistSongMaps.selectAll()
            .where { PlaylistSongMaps.playlistId eq playlistId }
            .orderBy(PlaylistSongMaps.position to SortOrder.ASC, PlaylistSongMaps.id to SortOrder.ASC)
            .map { it[PlaylistSongMaps.id] }
        orderedIds.forEachIndexed { index, id ->
            PlaylistSongMaps.update({ PlaylistSongMaps.id eq id }) { it[position] = index }
        }
    }

    // === ALBUMS ===

    fun getAllAlbums(): List<AlbumRow> = transaction {
        Albums.selectAll().orderBy(Albums.title).map { it.toAlbum() }
    }

    fun getAlbumSongs(albumId: String): List<SongRow> = transaction {
        (SongAlbumMaps innerJoin Songs)
            .selectAll()
            .where { SongAlbumMaps.albumId eq albumId }
            .orderBy(SongAlbumMaps.index)
            .map { it.toSong() }
    }

    // === ARTISTS ===

    fun getAllArtists(): List<ArtistRow> = transaction {
        Artists.selectAll().orderBy(Artists.name).map { it.toArtist() }
    }

    fun getArtistSongs(artistId: String): List<SongRow> = transaction {
        (SongArtistMaps innerJoin Songs)
            .selectAll()
            .where { SongArtistMaps.artistId eq artistId }
            .orderBy(SongArtistMaps.position)
            .map { it.toSong() }
    }

    // === EVENTS (LISTEN HISTORY) ===

    fun recordPlay(songId: String, playTimeMs: Long) = transaction {
        val now = (System.currentTimeMillis() / 1000).toInt()
        Events.insert {
            it[this.songId] = songId
            it[timestamp] = now
            it[playTime] = playTimeMs
        }
    }

    fun getRecentPlayedSongs(limit: Int = 20): List<SongRow> = transaction {
        if (limit <= 0) return@transaction emptyList()
        val recentIds = Events.selectAll()
            .orderBy(Events.timestamp to SortOrder.DESC, Events.id to SortOrder.DESC)
            .map { it[Events.songId] }
            .distinct()
            .take(limit)
        if (recentIds.isEmpty()) return@transaction emptyList()

        val songsById = Songs.selectAll()
            .where { Songs.id inList recentIds }
            .associate { row -> row[Songs.id] to row.toSongWithArtists() }
        recentIds.mapNotNull(songsById::get)
    }

    // === SEARCH HISTORY ===

    fun addSearchQuery(query: String) = transaction {
        val normalized = query.trim()
        if (normalized.isBlank()) return@transaction
        val matchingIds = SearchHistories.selectAll()
            .where { SearchHistories.query.lowerCase() eq normalized.lowercase() }
            .map { it[SearchHistories.id] }
        matchingIds.forEach { matchingId ->
            SearchHistories.deleteWhere { SearchHistories.id eq matchingId }
        }
        SearchHistories.insert {
            it[this.query] = normalized
        }
    }

    fun getSearchHistory(): List<String> = transaction {
        SearchHistories.selectAll()
            .orderBy(SearchHistories.id, SortOrder.DESC)
            .limit(20)
            .map { it[SearchHistories.query] }
    }

    fun clearSearchHistory() = transaction {
        SearchHistories.deleteAll()
    }
}

// === ROW DATA CLASSES ===

data class SongRow(
    val id: String,
    val title: String,
    val duration: Int = -1,
    val thumbnailUrl: String? = null,
    val albumId: String? = null,
    val albumName: String? = null,
    val explicit: Boolean = false,
    val year: Int? = null,
    val liked: Boolean = false,
    val likedDate: Int? = null,
    val totalPlayTime: Long = 0,
    val inLibrary: Int? = null,
    val isLocal: Boolean = false,
    val isDownloaded: Boolean = false,
    val isVideo: Boolean = false,
    val playbackPosition: Long? = null,
    val artists: List<String>? = null,
    val album: String? = null,
)

data class PlaylistRow(
    val id: String,
    val name: String,
    val browseId: String? = null,
    val createdAt: Int? = null,
    val isEditable: Boolean = true,
    val bookmarkedAt: Int? = null,
    val thumbnailUrl: String? = null,
    val isLocal: Boolean = false,
)

data class AlbumRow(
    val id: String,
    val title: String,
    val year: Int? = null,
    val thumbnailUrl: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val explicit: Boolean = false,
    val bookmarkedAt: Int? = null,
    val isLocal: Boolean = false,
)

data class ArtistRow(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val bookmarkedAt: Int? = null,
    val isLocal: Boolean = false,
)

// === RESULT ROW MAPPERS ===

fun ResultRow.toSong() = SongRow(
    id = this[Songs.id],
    title = this[Songs.title],
    duration = this[Songs.duration],
    thumbnailUrl = this[Songs.thumbnailUrl],
    albumId = this[Songs.albumId],
    albumName = this[Songs.albumName],
    explicit = this[Songs.explicit] == 1,
    year = this[Songs.year],
    liked = this[Songs.liked] == 1,
    likedDate = this[Songs.likedDate],
    totalPlayTime = this[Songs.totalPlayTime],
    inLibrary = this[Songs.inLibrary],
    isLocal = this[Songs.isLocal] == 1,
    isDownloaded = this[Songs.isDownloaded] == 1,
    isVideo = this[Songs.isVideo] == 1,
    playbackPosition = this[Songs.playbackPosition],
)

private fun ResultRow.toSongWithArtists(): SongRow {
    val song = toSong()
    val artistNames = (SongArtistMaps innerJoin Artists)
        .selectAll()
        .where { SongArtistMaps.songId eq song.id }
        .orderBy(SongArtistMaps.position)
        .map { it[Artists.name] }
    return song.copy(artists = artistNames, album = song.albumName)
}

fun ResultRow.toPlaylist() = PlaylistRow(
    id = this[Playlists.id],
    name = this[Playlists.name],
    browseId = this[Playlists.browseId],
    createdAt = this[Playlists.createdAt],
    isEditable = this[Playlists.isEditable] == 1,
    bookmarkedAt = this[Playlists.bookmarkedAt],
    thumbnailUrl = this[Playlists.thumbnailUrl],
    isLocal = this[Playlists.isLocal] == 1,
)

fun ResultRow.toAlbum() = AlbumRow(
    id = this[Albums.id],
    title = this[Albums.title],
    year = this[Albums.year],
    thumbnailUrl = this[Albums.thumbnailUrl],
    songCount = this[Albums.songCount],
    duration = this[Albums.duration],
    explicit = this[Albums.explicit] == 1,
    bookmarkedAt = this[Albums.bookmarkedAt],
    isLocal = this[Albums.isLocal] == 1,
)

fun ResultRow.toArtist() = ArtistRow(
    id = this[Artists.id],
    name = this[Artists.name],
    thumbnailUrl = this[Artists.thumbnailUrl],
    bookmarkedAt = this[Artists.bookmarkedAt],
    isLocal = this[Artists.isLocal] == 1,
)
