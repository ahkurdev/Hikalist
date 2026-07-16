package com.metrolist.desktop.player

import com.metrolist.desktop.preferences.AudioQualityPreference
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeClient
import com.metrolist.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

object DesktopYTPlayerUtils {
    private val logger = LoggerFactory.getLogger(DesktopYTPlayerUtils::class.java)

    private val streamClients = listOf(
        YouTubeClient.VISIONOS,
        YouTubeClient.ANDROID_VR_1_43_32,
        YouTubeClient.ANDROID_VR_1_61_48,
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.IOS,
        YouTubeClient.IPADOS,
    )

    data class PlaybackData(
        val streamUrl: String,
        val mimeType: String,
        val bitrate: Int,
        val itag: Int,
        val durationSeconds: Int,
        val title: String,
        val author: String,
        val videoId: String,
        val streamClient: String,
        val userAgent: String,
    )

    private data class StreamCandidate(
        val response: PlayerResponse,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    /** Fetch visitorData without requiring a YouTube account. */
    suspend fun ensureVisitorData() {
        if (YouTube.visitorData != null) return
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://music.youtube.com")
                    .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    .build()
                http.newCall(request).execute().use { response ->
                    val cookies = response.header("Set-Cookie") ?: response.header("set-cookie")
                    Regex("""VISITOR_INFO1_LIVE=([^;]+)""")
                        .find(cookies.orEmpty())
                        ?.groupValues
                        ?.get(1)
                        ?.let { visitorData ->
                            YouTube.visitorData = visitorData
                            logger.info("Got visitorData from cookie: {}", visitorData.take(12))
                        }

                    val body = response.body.string()
                    Regex("""visitorData\":\"([^\"]+)""")
                        .find(body)
                        ?.groupValues
                        ?.get(1)
                        ?.let { visitorData ->
                            YouTube.visitorData = visitorData
                            logger.info("Got visitorData from HTML: {}", visitorData.take(12))
                        }
                }
            } catch (e: Exception) {
                logger.warn("Failed to get visitorData: {}", e.message)
            }
        }
    }

    suspend fun resolveStream(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQualityPreference = AudioQualityPreference.AUTO,
    ): Result<PlaybackData> = withContext(Dispatchers.IO) {
        runCatching {
            ensureVisitorData()
            logger.debug(
                "Resolving stream: videoId={} visitorData={}",
                videoId,
                YouTube.visitorData?.take(12),
            )

            val selected = StreamResolutionPolicy.select(
                candidates = streamClients,
                resolve = { client -> resolveCandidate(videoId, playlistId, client, audioQuality) },
                validate = { client, candidate -> validateStream(candidate.streamUrl, client) },
            ) ?: throw RuntimeException("No validated anonymous YouTube stream was found")

            val client = selected.candidate
            val candidate = selected.value
            val response = candidate.response
            val format = candidate.format
            val clientLabel = client.friendlyName ?: client.clientName

            logger.info(
                "Validated stream: client={} format={} {}kbps itag={}",
                clientLabel,
                format.mimeType,
                format.bitrate / 1000,
                format.itag,
            )

            PlaybackData(
                streamUrl = candidate.streamUrl,
                mimeType = format.mimeType,
                bitrate = format.bitrate,
                itag = format.itag,
                durationSeconds = response.videoDetails?.lengthSeconds?.toInt() ?: 0,
                title = response.videoDetails?.title ?: "Unknown",
                author = response.videoDetails?.author ?: "Unknown",
                videoId = response.videoDetails?.videoId ?: videoId,
                streamClient = clientLabel,
                userAgent = client.userAgent,
            )
        }
    }

    private suspend fun resolveCandidate(
        videoId: String,
        playlistId: String?,
        client: YouTubeClient,
        audioQuality: AudioQualityPreference,
    ): StreamCandidate? {
        val clientLabel = client.friendlyName ?: client.clientName
        logger.debug("Trying anonymous stream client: {}", clientLabel)

        val response = YouTube.player(videoId, playlistId, client, null, null)
            .onFailure { logger.warn("Player request failed for {}: {}", clientLabel, it.message) }
            .getOrNull()
            ?: return null

        if (response.playabilityStatus.status != "OK") {
            logger.debug(
                "Client {} rejected playback: {}",
                clientLabel,
                response.playabilityStatus.reason,
            )
            return null
        }

        val audioFormats = response.streamingData
            ?.adaptiveFormats
            .orEmpty()
            .filter { it.isAudio && !it.url.isNullOrBlank() }
        val preferredCodec = audioFormats.maxOfOrNull { codecPreference(it.mimeType) }
        val format = preferredCodec?.let { codecScore ->
            AudioQualityPolicy.select(
                candidates = audioFormats.filter { codecPreference(it.mimeType) == codecScore },
                quality = audioQuality,
                bitrateOf = { it.bitrate },
            )
        }

        if (format == null) {
            logger.debug("Client {} returned no direct audio URL", clientLabel)
            return null
        }

        return StreamCandidate(
            response = response,
            format = format,
            streamUrl = requireNotNull(format.url),
        )
    }

    private fun codecPreference(mimeType: String): Int = when {
        mimeType.contains("mp4a", ignoreCase = true) -> 2
        mimeType.contains("opus", ignoreCase = true) -> 1
        else -> 0
    }

    private fun validateStream(
        streamUrl: String,
        client: YouTubeClient,
    ): Boolean {
        val clientLabel = client.friendlyName ?: client.clientName
        val request = Request.Builder()
            .url(streamUrl)
            .header("User-Agent", client.userAgent)
            .header("Range", "bytes=0-1023")
            .header("Accept-Encoding", "identity")
            .build()

        return try {
            http.newCall(request).execute().use { response ->
                val valid = response.code == 200 || response.code == 206
                if (!valid) {
                    logger.debug("Stream validation failed: client={} HTTP {}", clientLabel, response.code)
                }
                valid
            }
        } catch (e: Exception) {
            logger.debug("Stream validation failed: client={} {}", clientLabel, e.message)
            false
        }
    }
}
