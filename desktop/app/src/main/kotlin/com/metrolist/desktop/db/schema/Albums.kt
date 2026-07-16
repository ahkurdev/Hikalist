package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.Table

object Albums : Table("album") {
    val id = text("id")
    val playlistId = text("playlistId").nullable()
    val title = text("title")
    val year = integer("year").nullable()
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val songCount = integer("songCount")
    val duration = integer("duration")
    val explicit = integer("explicit").default(0)
    val bookmarkedAt = integer("bookmarkedAt").nullable()
    val likedDate = integer("likedDate").nullable()
    val inLibrary = integer("inLibrary").nullable()
    val isLocal = integer("isLocal").default(0)
    val isUploaded = integer("isUploaded").default(0)

    override val primaryKey = PrimaryKey(id)
}
