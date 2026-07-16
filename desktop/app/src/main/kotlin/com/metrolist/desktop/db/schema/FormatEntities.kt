package com.metrolist.desktop.db.schema

import org.jetbrains.exposed.sql.Table

object FormatEntities : Table("format") {
    val id = text("id")
    val itag = integer("itag")
    val mimeType = text("mimeType")
    val codecs = text("codecs")
    val bitrate = integer("bitrate")
    val sampleRate = integer("sampleRate").nullable()
    val contentLength = long("contentLength")
    val loudnessDb = double("loudnessDb").nullable()
    val perceptualLoudnessDb = double("perceptualLoudnessDb").nullable()
    val playbackUrl = text("playbackUrl").nullable()

    override val primaryKey = PrimaryKey(id)
}
