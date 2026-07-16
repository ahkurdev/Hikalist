package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.Table

object Songs : Table("song") {
    val id = text("id")
    val title = text("title")
    val duration = integer("duration").default(-1)
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val albumId = text("albumId").nullable().index()
    val albumName = text("albumName").nullable()
    val explicit = integer("explicit").default(0)
    val year = integer("year").nullable()
    val date = integer("date").nullable()
    val dateModified = integer("dateModified").nullable()
    val liked = integer("liked").default(0)
    val likedDate = integer("likedDate").nullable()
    val totalPlayTime = long("totalPlayTime").default(0)
    val inLibrary = integer("inLibrary").nullable()
    val dateDownload = integer("dateDownload").nullable()
    val isLocal = integer("isLocal").default(0)
    val libraryAddToken = text("libraryAddToken").nullable()
    val libraryRemoveToken = text("libraryRemoveToken").nullable()
    val lyricsOffset = integer("lyricsOffset").default(0)
    val romanizeLyrics = integer("romanizeLyrics").default(1)
    val isDownloaded = integer("isDownloaded").default(0)
    val isUploaded = integer("isUploaded").default(0)
    val isVideo = integer("isVideo").default(0)
    val isEpisode = integer("isEpisode").default(0)
    val playbackPosition = long("playbackPosition").nullable()
    val uploadEntityId = text("uploadEntityId").nullable()
    val isCached = integer("isCached").default(0)

    override val primaryKey = PrimaryKey(id)
}
