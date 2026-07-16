package com.metrolist.desktop.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PlaylistSyncPayload(
    val name: String,
    val description: String,
    val coverUrl: String?,
    val deletedAtEpochSeconds: Long?,
)

@Serializable
data class SongSyncPayload(
    val title: String,
    val artist: String,
    val album: String?,
    val thumbnailUrl: String?,
    val duration: Int,
    val position: Int,
    val addedByName: String,
    val addedAtEpochSeconds: Long,
    val deletedAtEpochSeconds: Long?,
)

object SyncPayloadCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun encode(payload: PlaylistSyncPayload): String = json.encodeToString(payload)

    fun encode(payload: SongSyncPayload): String = json.encodeToString(payload)

    fun decodePlaylist(payload: String): PlaylistSyncPayload = json.decodeFromString(payload)

    fun decodeSong(payload: String): SongSyncPayload = json.decodeFromString(payload)
}
