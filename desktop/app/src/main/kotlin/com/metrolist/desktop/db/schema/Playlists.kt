package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.Table

object Playlists : Table("playlist") {
    val id = text("id")
    val name = text("name")
    val browseId = text("browseId").nullable()
    val createdAt = integer("createdAt").nullable()
    val isEditable = integer("isEditable").default(1)
    val bookmarkedAt = integer("bookmarkedAt").nullable()
    val remoteSongCount = integer("remoteSongCount").nullable()
    val playEndpointParams = text("playEndpointParams").nullable()
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val shuffleEndpointParams = text("shuffleEndpointParams").nullable()
    val radioEndpointParams = text("radioEndpointParams").nullable()
    val isLocal = integer("isLocal").default(0)
    val isAutoSync = integer("isAutoSync").default(0)

    override val primaryKey = PrimaryKey(id)
}
