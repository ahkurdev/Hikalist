package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object LyricsEntities : Table("lyrics") {
    val id = text("id")
    val lyrics = text("lyrics")
    val provider = text("provider").default("Unknown")
    val translatedLyrics = text("translatedLyrics").default("")
    val translationLanguage = text("translationLanguage").default("")
    val translationMode = text("translationMode").default("")

    override val primaryKey = PrimaryKey(id)
}

object Events : Table("event") {
    val id = long("id").autoIncrement()
    val songId = text("songId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()
    val timestamp = integer("timestamp")
    val playTime = long("playTime")

    override val primaryKey = PrimaryKey(id)
}

object SearchHistories : Table("search_history") {
    val id = long("id").autoIncrement()
    val query = text("query").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

object PlayCountEntities : Table("playCount") {
    val songId = text("song")
    val year = integer("year").default(-1)
    val month = integer("month").default(-1)
    val count = integer("count").default(-1)

    override val primaryKey = PrimaryKey(songId, year, month)
}

object RelatedSongMaps : Table("related_song_map") {
    val id = long("id").autoIncrement()
    val songId = text("songId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()
    val relatedSongId = text("relatedSongId").references(Songs.id, onDelete = ReferenceOption.CASCADE).index()

    override val primaryKey = PrimaryKey(id)
}

object SetVideoIds : Table("set_video_id") {
    val videoId = text("videoId")
    val setVideoId = text("setVideoId").nullable()

    override val primaryKey = PrimaryKey(videoId)
}

object SpeedDialItems : Table("speed_dial_item") {
    val id = text("id")
    val secondaryId = text("secondaryId").nullable()
    val title = text("title")
    val subtitle = text("subtitle").nullable()
    val subtitleIds = text("subtitleIds").nullable()
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val type = text("type")
    val explicit = integer("explicit").default(0)
    val createDate = long("createDate")
    val albumId = text("albumId").nullable()
    val albumName = text("albumName").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Podcasts : Table("podcast") {
    val id = text("id")
    val title = text("title")
    val author = text("author").nullable()
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val channelId = text("channelId").nullable()
    val bookmarkedAt = integer("bookmarkedAt").nullable()

    override val primaryKey = PrimaryKey(id)
}

object RecognitionHistories : Table("recognition_history") {
    val id = long("id").autoIncrement()
    val trackId = text("trackId").index()
    val title = text("title")
    val artist = text("artist")
    val album = text("album").nullable()
    val coverArtUrl = text("coverArtUrl").nullable()
    val coverArtHqUrl = text("coverArtHqUrl").nullable()
    val genre = text("genre").nullable()
    val recognizedAt = integer("recognizedAt")
    val liked = integer("liked").default(0)

    override val primaryKey = PrimaryKey(id)
}
