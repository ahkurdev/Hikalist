package com.metrolist.desktop.db

import com.metrolist.desktop.db.schema.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseFactory {
    private var initialized = false

    fun init(dbPath: String = "${System.getProperty("user.home")}/.metrolist/song.db") {
        if (initialized) return
        synchronized(this) {
            if (initialized) return

            File(dbPath).parentFile.mkdirs()

            Database.connect(
                url = "jdbc:sqlite:$dbPath",
                driver = "org.sqlite.JDBC"
            )

            transaction {
                SchemaUtils.create(
                    Songs,
                    Artists,
                    Albums,
                    Playlists,
                    SongArtistMaps,
                    SongAlbumMaps,
                    AlbumArtistMaps,
                    PlaylistSongMaps,
                    FormatEntities,
                    LyricsEntities,
                    Events,
                    SearchHistories,
                    PlayCountEntities,
                    RelatedSongMaps,
                    SetVideoIds,
                    SpeedDialItems,
                    Podcasts,
                    RecognitionHistories
                )
            }

            initialized = true
        }
    }
}
