package com.metrolist.desktop.offline

import com.metrolist.desktop.lyrics.LyricsRequest
import com.metrolist.desktop.lyrics.LyricsResolver
import com.metrolist.desktop.player.DesktopYTPlayerUtils
import com.metrolist.desktop.preferences.AudioQualityPreference
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class OfflineDownloadRequest(
    val metadata: OfflineSongMetadata,
    val playlistId: String? = null,
)

data class ResolvedOfflineStream(
    val url: String,
    val mimeType: String,
    val userAgent: String,
)

sealed interface OfflineDownloadState {
    data object Queued : OfflineDownloadState

    data class Downloading(
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : OfflineDownloadState {
        val progress: Float?
            get() = totalBytes.takeIf { it > 0L }?.let { (downloadedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() }
    }

    data class Downloaded(val media: OfflineMedia) : OfflineDownloadState
    data class Failed(val message: String) : OfflineDownloadState
    data object Cancelled : OfflineDownloadState
}

fun interface OfflineStreamResolver {
    suspend fun resolve(request: OfflineDownloadRequest): ResolvedOfflineStream
}

fun interface OfflineAudioTransfer {
    suspend fun download(
        stream: ResolvedOfflineStream,
        destination: File,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit,
    )
}

fun interface OfflineLyricsFetcher {
    suspend fun fetch(metadata: OfflineSongMetadata): Boolean
}

fun interface OfflineCoverTransfer {
    suspend fun download(url: String, destination: File): Boolean
}

class OfflineDownloadManager(
    private val store: OfflineMediaStore,
    private val streamResolver: OfflineStreamResolver,
    private val audioTransfer: OfflineAudioTransfer,
    private val lyricsFetcher: OfflineLyricsFetcher,
    private val coverTransfer: OfflineCoverTransfer = OfflineCoverTransfer { _, _ -> false },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : AutoCloseable {
    private val queue = Channel<OfflineDownloadRequest>(Channel.UNLIMITED)
    private val queuedIds = ConcurrentHashMap.newKeySet<String>()
    private val cancelledIds = ConcurrentHashMap.newKeySet<String>()
    private val stateLock = Any()
    private val _states = kotlinx.coroutines.flow.MutableStateFlow<Map<String, OfflineDownloadState>>(emptyMap())
    val states: kotlinx.coroutines.flow.StateFlow<Map<String, OfflineDownloadState>> = _states

    init {
        scope.launch {
            for (request in queue) {
                queuedIds.remove(request.metadata.videoId)
                process(request)
            }
        }
    }

    fun enqueue(request: OfflineDownloadRequest) = enqueue(listOf(request))

    fun enqueue(requests: List<OfflineDownloadRequest>) {
        requests.distinctBy { it.metadata.videoId }.forEach { request ->
            val videoId = request.metadata.videoId
            store.find(videoId)?.let { media ->
                publish(videoId, OfflineDownloadState.Downloaded(media))
                return@forEach
            }
            cancelledIds.remove(videoId)
            val current = states.value[videoId]
            if (current is OfflineDownloadState.Downloading || !queuedIds.add(videoId)) return@forEach
            publish(videoId, OfflineDownloadState.Queued)
            queue.trySend(request)
        }
    }

    fun cancel(videoId: String) {
        cancelledIds.add(videoId)
        publish(videoId, OfflineDownloadState.Cancelled)
    }

    fun remove(videoId: String) {
        cancel(videoId)
        store.remove(videoId)
        synchronized(stateLock) { _states.value = _states.value - videoId }
    }

    fun isDownloaded(videoId: String): Boolean = store.isDownloaded(videoId)

    fun media(videoId: String): OfflineMedia? = store.find(videoId)

    private suspend fun process(request: OfflineDownloadRequest) {
        val videoId = request.metadata.videoId
        if (cancelledIds.remove(videoId)) return
        store.find(videoId)?.let { media ->
            publish(videoId, OfflineDownloadState.Downloaded(media))
            return
        }

        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) {
            val temporaryAudio = store.temporaryAudioFile(videoId)
            val temporaryCover = store.temporaryCoverFile(videoId)
            try {
                ensureNotCancelled(videoId)
                publish(videoId, OfflineDownloadState.Downloading(0L, 0L))
                val stream = streamResolver.resolve(request)
                require(stream.mimeType.contains("audio/mp4", ignoreCase = true) && stream.mimeType.contains("mp4a", ignoreCase = true)) {
                    "This song did not provide an AAC/M4A stream"
                }
                audioTransfer.download(stream, temporaryAudio) { downloaded, total ->
                    if (videoId in cancelledIds) throw CancellationException("Download cancelled")
                    publish(videoId, OfflineDownloadState.Downloading(downloaded, total))
                }
                ensureNotCancelled(videoId)
                val cover = request.metadata.thumbnailUrl?.let { url ->
                    runCatching { coverTransfer.download(url, temporaryCover) }
                        .getOrDefault(false)
                        .takeIf { it }
                        ?.let { temporaryCover }
                }
                val lyricsAvailable = runCatching { lyricsFetcher.fetch(request.metadata) }.getOrDefault(false)
                ensureNotCancelled(videoId)
                val media = store.commit(request.metadata, temporaryAudio, lyricsAvailable, cover)
                publish(videoId, OfflineDownloadState.Downloaded(media))
                return
            } catch (cancelled: CancellationException) {
                temporaryAudio.delete()
                temporaryCover.delete()
                publish(videoId, OfflineDownloadState.Cancelled)
                cancelledIds.remove(videoId)
                return
            } catch (error: Throwable) {
                temporaryAudio.delete()
                temporaryCover.delete()
                lastError = error
            }
        }
        publish(videoId, OfflineDownloadState.Failed(lastError?.message ?: "Download failed"))
    }

    private suspend fun ensureNotCancelled(videoId: String) {
        currentCoroutineContext().ensureActive()
        if (videoId in cancelledIds) throw CancellationException("Download cancelled")
    }

    private fun publish(videoId: String, state: OfflineDownloadState) {
        synchronized(stateLock) { _states.value = _states.value + (videoId to state) }
    }

    override fun close() {
        queue.close()
        scope.cancel()
    }

    companion object {
        private const val MAX_ATTEMPTS = 2

        fun default(
            store: OfflineMediaStore,
            lyricsResolver: LyricsResolver,
            audioQualityProvider: () -> AudioQualityPreference = { AudioQualityPreference.AUTO },
        ): OfflineDownloadManager = OfflineDownloadManager(
            store = store,
            streamResolver = OfflineStreamResolver { request ->
                val stream = DesktopYTPlayerUtils.resolveStream(
                    videoId = request.metadata.videoId,
                    playlistId = request.playlistId,
                    audioQuality = audioQualityProvider(),
                ).getOrThrow()
                ResolvedOfflineStream(stream.streamUrl, stream.mimeType, stream.userAgent)
            },
            audioTransfer = OkHttpOfflineAudioTransfer(),
            lyricsFetcher = OfflineLyricsFetcher { metadata ->
                lyricsResolver.resolve(
                    LyricsRequest(
                        videoId = metadata.videoId,
                        title = metadata.title,
                        artist = metadata.artist,
                        durationSeconds = metadata.durationSeconds,
                        album = metadata.album,
                    ),
                ) != null
            },
            coverTransfer = OkHttpOfflineCoverTransfer(),
        )
    }
}

private class OkHttpOfflineAudioTransfer(
    private val client: OkHttpClient = downloadClient(),
) : OfflineAudioTransfer {
    override suspend fun download(
        stream: ResolvedOfflineStream,
        destination: File,
        onProgress: (Long, Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val request = Request.Builder()
            .url(stream.url)
            .header("User-Agent", stream.userAgent)
            .header("Accept-Encoding", "identity")
            .build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "Audio download failed with HTTP ${response.code}" }
            val body = response.body
            val totalBytes = body.contentLength()
            body.byteStream().buffered().use { input ->
                destination.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var downloaded = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, totalBytes)
                    }
                }
            }
        }
        check(destination.length() > 0L) { "Downloaded audio is empty" }
    }
}

private class OkHttpOfflineCoverTransfer(
    private val client: OkHttpClient = downloadClient(),
) : OfflineCoverTransfer {
    override suspend fun download(url: String, destination: File): Boolean = withContext(Dispatchers.IO) {
        destination.parentFile?.mkdirs()
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext false
            response.body.byteStream().use { input ->
                destination.outputStream().use(input::copyTo)
            }
        }
        destination.length() > 0L
    }
}

private fun downloadClient() = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .followRedirects(true)
    .build()
