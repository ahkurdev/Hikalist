package com.metrolist.desktop.player

internal object PlaybackSeekPolicy {
    private const val ENDPOINT_PREVIEW_SECONDS = 2.0
    private const val NATURAL_END_TOLERANCE_SECONDS = 1.5

    fun normalize(requestedSeconds: Double, durationSeconds: Double): Double {
        if (durationSeconds <= 0.0) return 0.0
        val latestPlayablePosition = (durationSeconds - ENDPOINT_PREVIEW_SECONDS).coerceAtLeast(0.0)
        return requestedSeconds.coerceIn(0.0, latestPlayablePosition)
    }

    fun reachedNaturalEnd(positionSeconds: Double, durationSeconds: Double): Boolean {
        if (durationSeconds <= 0.0) return positionSeconds > 0.0
        return positionSeconds >= durationSeconds - NATURAL_END_TOLERANCE_SECONDS
    }
}

internal class PlaybackStreamCache<T> {
    private var videoId: String? = null
    private var value: T? = null

    @Synchronized
    fun get(videoId: String): T? = value?.takeIf { this.videoId == videoId }

    @Synchronized
    fun put(videoId: String, value: T) {
        this.videoId = videoId
        this.value = value
    }

    @Synchronized
    fun clear() {
        videoId = null
        value = null
    }
}
