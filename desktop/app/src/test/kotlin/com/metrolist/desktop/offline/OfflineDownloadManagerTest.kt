package com.metrolist.desktop.offline

import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDownloadManagerTest {
    @Test
    fun `download stores m4a and caches lyrics before publishing completion`() = runBlocking {
        val root = Files.createTempDirectory("hikalist-download-manager").toFile()
        val events = mutableListOf<String>()
        val manager = OfflineDownloadManager(
            store = OfflineMediaStore(root),
            streamResolver = OfflineStreamResolver {
                events += "resolve"
                ResolvedOfflineStream("https://example.com/audio", "audio/mp4; codecs=\"mp4a.40.2\"", "Hikalist")
            },
            audioTransfer = OfflineAudioTransfer { _, destination, onProgress ->
                events += "audio"
                destination.parentFile.mkdirs()
                destination.writeBytes(ByteArray(8) { it.toByte() })
                onProgress(8, 8)
            },
            lyricsFetcher = OfflineLyricsFetcher {
                events += "lyrics"
                true
            },
        )

        try {
            manager.enqueue(request("song-1"))
            val completed = manager.awaitDownloaded("song-1")

            assertEquals(listOf("resolve", "audio", "lyrics"), events)
            assertEquals(8L, completed.media.audioFile.length())
            assertTrue(completed.media.lyricsAvailable)
        } finally {
            manager.close()
        }
    }

    @Test
    fun `playlist downloads sequentially and skips an existing song`() = runBlocking {
        val root = Files.createTempDirectory("hikalist-download-playlist").toFile()
        val store = OfflineMediaStore(root)
        commitExisting(store, "song-1")
        val resolved = mutableListOf<String>()
        val manager = OfflineDownloadManager(
            store = store,
            streamResolver = OfflineStreamResolver { request ->
                resolved += request.metadata.videoId
                ResolvedOfflineStream("https://example.com/${request.metadata.videoId}", "audio/mp4; codecs=mp4a.40.2", "Hikalist")
            },
            audioTransfer = OfflineAudioTransfer { _, destination, onProgress ->
                destination.parentFile.mkdirs()
                destination.writeBytes(byteArrayOf(1, 2, 3))
                onProgress(3, 3)
            },
            lyricsFetcher = OfflineLyricsFetcher { false },
        )

        try {
            manager.enqueue(listOf(request("song-1"), request("song-2"), request("song-3")))
            manager.awaitDownloaded("song-2")
            manager.awaitDownloaded("song-3")

            assertEquals(listOf("song-2", "song-3"), resolved)
            assertTrue(manager.isDownloaded("song-1"))
            assertTrue(manager.isDownloaded("song-2"))
            assertTrue(manager.isDownloaded("song-3"))
        } finally {
            manager.close()
        }
    }

    @Test
    fun `non aac stream is rejected without leaving a partial file`() = runBlocking {
        val root = Files.createTempDirectory("hikalist-download-codec").toFile()
        val store = OfflineMediaStore(root)
        val manager = OfflineDownloadManager(
            store = store,
            streamResolver = OfflineStreamResolver {
                ResolvedOfflineStream("https://example.com/opus", "audio/webm; codecs=opus", "Hikalist")
            },
            audioTransfer = OfflineAudioTransfer { _, _, _ -> error("transfer must not start") },
            lyricsFetcher = OfflineLyricsFetcher { false },
        )

        try {
            manager.enqueue(request("song-opus"))
            val failed = withTimeout(5_000) {
                manager.states.mapNotNull { it["song-opus"] }.filterIsInstance<OfflineDownloadState.Failed>().first()
            }

            assertTrue(failed.message.contains("AAC", ignoreCase = true))
            assertTrue(!store.temporaryAudioFile("song-opus").exists())
        } finally {
            manager.close()
        }
    }

    private suspend fun OfflineDownloadManager.awaitDownloaded(videoId: String): OfflineDownloadState.Downloaded =
        withTimeout(5_000) {
            states.mapNotNull { it[videoId] }.filterIsInstance<OfflineDownloadState.Downloaded>().first()
        }

    private fun request(videoId: String) = OfflineDownloadRequest(
        metadata = OfflineSongMetadata(
            videoId = videoId,
            title = "Title $videoId",
            artist = "Artist",
            album = "Album",
            thumbnailUrl = null,
            durationSeconds = 180,
        ),
    )

    private fun commitExisting(store: OfflineMediaStore, videoId: String) {
        val temporaryAudio = store.temporaryAudioFile(videoId)
        temporaryAudio.parentFile.mkdirs()
        temporaryAudio.writeBytes(byteArrayOf(1))
        store.commit(request(videoId).metadata, temporaryAudio, lyricsAvailable = false)
    }
}
