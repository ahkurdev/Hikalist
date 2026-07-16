package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object SongArtistMaps : Table("song_artist_map") {
    val songId = text("songId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()
    val artistId = text("artistId").references(Artists.id, onDelete = ReferenceOption.CASCADE).index()
    val position = integer("position")

    override val primaryKey = PrimaryKey(songId, artistId)
}

object SongAlbumMaps : Table("song_album_map") {
    val songId = text("songId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()
    val albumId = text("albumId").references(Albums.id, onDelete = ReferenceOption.CASCADE).index()
    val index = integer("index")

    override val primaryKey = PrimaryKey(songId, albumId)
}

object AlbumArtistMaps : Table("album_artist_map") {
    val albumId = text("albumId").references(Albums.id, onDelete = ReferenceOption.CASCADE).index()
    val artistId = text("artistId").references(Artists.id, onDelete = ReferenceOption.CASCADE).index()
    val order = integer("order")

    override val primaryKey = PrimaryKey(albumId, artistId)
}

object PlaylistSongMaps : Table("playlist_song_map") {
    val id = integer("id").autoIncrement()
    val playlistId = text("playlistId").references(Playlists.id, onDelete = ReferenceOption.CASCADE).index()
    val songId = text("songId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()
    val position = integer("position").default(0)
    val setVideoId = text("setVideoId").nullable()

    override val primaryKey = PrimaryKey(id)
}
