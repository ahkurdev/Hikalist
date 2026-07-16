package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.Table

object Artists : Table("artist") {
    val id = text("id")
    val name = text("name")
    val thumbnailUrl = text("thumbnailUrl").nullable()
    val channelId = text("channelId").nullable()
    val bookmarkedAt = integer("bookmarkedAt").nullable()
    val isLocal = integer("isLocal").default(0)
    val isPodcastChannel = integer("isPodcastChannel").default(0)
    val cachedPageJson = text("cachedPageJson").nullable()

    override val primaryKey = PrimaryKey(id)
}
