package com.metrolist.desktop.player

import com.metrolist.desktop.offline.OfflineMedia
import com.metrolist.desktop.preferences.AudioQualityPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.FloatControl
import javax.sound.sampled.SourceDataLine
import kotlin.math.log10

class DesktopMusicPlayer(
    ffmpegPathProvider: () -> String? = ::resolveFfmpegPath,
    initialVolumePercent: Int = 100,
    initialRepeatMode: RepeatMode = RepeatMode.ALL,
    private val audioQualityProvider: () -> AudioQualityPreference = { AudioQualityPreference.AUTO },
    private val offlineMediaProvider: (videoId: String) -> OfflineMedia? = { null },
    initialEqualizerConfiguration: EqualizerConfiguration = EqualizerConfiguration(),
    private val queueSessionStore: QueueSessionStore? = null,
    private val audioOutputProvider: AudioOutputProvider = JavaSoundAudioOutputProvider(),
    initialAudioOutputId: String = AudioOutputDevice.SYSTEM_DEFAULT_ID,
) {
    private val logger = LoggerFactory.getLogger(DesktopMusicPlayer::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val playbackQueue = PlaybackQueue()
    private val playbackGeneration = AtomicLong(0)
    private val endOfTrackSleepLatch = EndOfTrackSleepLatch()

    private var job: Job? = null
    private var process: Process? = null
    private var line: SourceDataLine? = null
    @Volatile private var paused = false
    private var appliedVolumePercent = initialVolumePercent.coerceIn(0, 100)
    private val ffmpegPath: String? = runCatching(ffmpegPathProvider).getOrElse { error ->
        logger.warn("Unable to locate FFmpeg", error)
        null
    }
    private val retryPolicy = PlaybackRetryPolicy()
    private val streamCache = PlaybackStreamCache<DesktopYTPlayerUtils.PlaybackData>()
    private val equalizer = AudioEqualizer(SAMPLE_RATE, CHANNELS).apply { update(initialEqualizerConfiguration) }
    @Volatile private var selectedAudioOutputId = AudioOutputSelection.resolve(
        initialAudioOutputId,
        runCatching(audioOutputProvider::devices).getOrDefault(emptyList()),
    )
    private val _audioOutputId = MutableStateFlow(selectedAudioOutputId)
    val audioOutputId: StateFlow<String> = _audioOutputId.asStateFlow()

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state.asStateFlow()
    private val _position = MutableStateFlow(0.0)
    val position: StateFlow<Double> = _position.asStateFlow()
    private val _duration = MutableStateFlow(0.0)
    val duration: StateFlow<Double> = _duration.asStateFlow()
    private val _currentSong = MutableStateFlow<NowPlaying?>(null)
    val currentSong: StateFlow<NowPlaying?> = _currentSong.asStateFlow()
    private val _queueItems = MutableStateFlow<List<QueueItem>>(emptyList())
    val queueItems: StateFlow<List<QueueItem>> = _queueItems.asStateFlow()
    private val _queueIndex = MutableStateFlow(-1)
    val queueIndex: StateFlow<Int> = _queueIndex.asStateFlow()
    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()
    private val _repeatMode = MutableStateFlow(initialRepeatMode)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    private val _volumePercent = MutableStateFlow(appliedVolumePercent)
    val volumePercent: StateFlow<Int> = _volumePercent.asStateFlow()
    private val _playbackNotice = MutableStateFlow<PlaybackNotice?>(null)
    val playbackNotice: StateFlow<PlaybackNotice?> = _playbackNotice.asStateFlow()

    enum class State { IDLE, PLAYING, PAUSED, BUFFERING, ERROR }

    data class QueueItem(
        val videoId: String,
        val playlistId: String? = null,
        val title: String = "",
        val artist: String = "",
        val album: String? = null,
        val thumbnailUrl: String? = null,
        val duration: Int = 0,
        val addedBy: String? = null,
        val addedAt: Long? = null,
    )

    data class NowPlaying(
        val item: QueueItem,
        val title: String,
        val artist: String,
        val thumbnailUrl: String? = null,
    )

    init {
        playbackQueue.repeatMode = initialRepeatMode
        queueSessionStore?.load()?.let(::restoreQueueSession)
        if (ffmpegPath == null) logger.warn("FFmpeg not found; audio playback is unavailable")
    }

    fun play(item: QueueItem) {
        setQueue(listOf(item), 0)
    }

    fun setQueue(items: List<QueueItem>, selectedIndex: Int = 0) {
        if (items.isEmpty()) return
        playbackQueue.setSource(items, selectedIndex)
        _shuffle.value = false
        publishQueue()
        playbackQueue.current?.let { startPlayback(it, 0.0) }
    }

    private fun startPlayback(item: QueueItem, startSeconds: Double, failedAttempt: Int = 0) {
        val offlineMedia = offlineMediaProvider(item.videoId)
        if (failedAttempt == 0) _playbackNotice.value = null
        _position.value = startSeconds.coerceAtLeast(0.0)
        _duration.value = (offlineMedia?.metadata?.durationSeconds ?: item.duration).toDouble().coerceAtLeast(0.0)
        _currentSong.value = NowPlaying(
            item = item,
            title = item.title,
            artist = item.artist,
            thumbnailUrl = offlineMedia?.coverFile?.absolutePath ?: item.thumbnailUrl,
        )
        saveQueueSession()

        val ffmpeg = ffmpegPath
        if (ffmpeg == null) {
            _playbackNotice.value = PlaybackNotice.Failed(
                PlaybackFailure(
                    kind = PlaybackFailureKind.DECODER,
                    title = "Playback engine unavailable",
                    message = "Hikalist could not load its bundled audio component.",
                    retryable = false,
                ),
            )
            _state.value = State.ERROR
            return
        }

        closeActivePlayback()
        val generation = playbackGeneration.incrementAndGet()
        paused = false
        _state.value = State.BUFFERING

        job = scope.launch {
            var completedNaturally = false
            try {
                val source = PlaybackSourceSelector(
                    offlineMediaProvider = { videoId -> offlineMedia?.takeIf { videoId == item.videoId } },
                    onlineResolver = {
                        (if (failedAttempt == 0) streamCache.get(item.videoId) else null)
                            ?: DesktopYTPlayerUtils.resolveStream(
                                videoId = item.videoId,
                                playlistId = item.playlistId,
                                audioQuality = audioQualityProvider(),
                            ).getOrThrow().also { streamCache.put(item.videoId, it) }
                    },
                ).resolve(item)
                if (generation != playbackGeneration.get()) return@launch
                _duration.value = source.durationSeconds.toDouble()

                val command = mutableListOf(
                    ffmpeg,
                    "-hide_banner", "-loglevel", "warning",
                )
                if (source is PlaybackSource.Online) {
                    command += listOf(
                        "-reconnect", "1", "-reconnect_streamed", "1", "-reconnect_delay_max", "5",
                        "-user_agent", source.stream.userAgent,
                    )
                }
                if (startSeconds > 0.0) {
                    command += listOf("-ss", "%.3f".format(java.util.Locale.US, startSeconds))
                }
                command += when (source) {
                    is PlaybackSource.Local -> listOf("-i", source.audioFile.absolutePath)
                    is PlaybackSource.Online -> listOf("-i", source.stream.streamUrl)
                }
                command += listOf(
                    "-vn", "-f", "s16le", "-acodec", "pcm_s16le",
                    "-ar", SAMPLE_RATE.toString(), "-ac", CHANNELS.toString(), "pipe:1",
                )

                val startedProcess = ProcessBuilder(command).redirectErrorStream(false).start()
                process = startedProcess
                val stderrJob = launch {
                    startedProcess.errorStream.bufferedReader().lineSequence().forEach { logger.warn("FFmpeg: {}", it) }
                }

                val audioFormat = AudioFormat(SAMPLE_RATE.toFloat(), 16, CHANNELS, true, false)
                val audioLine = audioOutputProvider.openLine(audioFormat, selectedAudioOutputId)
                line = audioLine
                audioLine.open(audioFormat, 16_384)
                applyVolume(audioLine)
                audioLine.start()
                _playbackNotice.value = null
                _state.value = State.PLAYING

                streamPcm(
                    input = startedProcess.inputStream,
                    audioLine = audioLine,
                    generation = generation,
                    startSeconds = startSeconds,
                )
                completedNaturally = generation == playbackGeneration.get() && currentCoroutineContext().isActive

                if (!startedProcess.waitFor(2, TimeUnit.SECONDS)) startedProcess.destroyForcibly()
                stderrJob.join()
                if (completedNaturally && !PlaybackSeekPolicy.reachedNaturalEnd(_position.value, source.durationSeconds.toDouble())) {
                    completedNaturally = false
                    error("FFmpeg ended at ${"%.2f".format(_position.value)}s before ${source.durationSeconds}s")
                }
                if (completedNaturally) audioLine.drain()
            } catch (error: Exception) {
                if (generation == playbackGeneration.get() && currentCoroutineContext().isActive) {
                    logger.error("Playback failed: {} {}", error::class.simpleName, error.message)
                    val failure = PlaybackFailureClassifier.classify(error)
                    when (val decision = retryPolicy.decide(failure, failedAttempt)) {
                        is RetryDecision.Retry -> {
                            _state.value = State.BUFFERING
                            _playbackNotice.value = PlaybackNotice.Retrying(
                                failure = failure,
                                attempt = decision.attempt,
                                maxAttempts = retryPolicy.maxRetries,
                            )
                            val retryPosition = _position.value
                            delay(decision.delayMillis)
                            if (generation == playbackGeneration.get() && currentCoroutineContext().isActive) {
                                scope.launch { startPlayback(item, retryPosition, decision.attempt) }
                            }
                        }
                        RetryDecision.Stop -> {
                            streamCache.clear()
                            _playbackNotice.value = PlaybackNotice.Failed(failure)
                            _state.value = State.ERROR
                        }
                    }
                }
            } finally {
                if (generation == playbackGeneration.get()) {
                    process?.destroyForcibly()
                    process = null
                    line?.close()
                    line = null
                    if (completedNaturally) playAfterCompletion()
                }
            }
        }
    }

    private suspend fun streamPcm(
        input: InputStream,
        audioLine: SourceDataLine,
        generation: Long,
        startSeconds: Double,
    ): Long {
        val buffer = ByteArray(8_192)
        val frameBuffer = PcmFrameBuffer(FRAME_SIZE)
        var totalBytes = 0L
        var persistedBucket = (startSeconds / SESSION_SAVE_INTERVAL_SECONDS).toLong()
        while (currentCoroutineContext().isActive && generation == playbackGeneration.get()) {
            while (paused && currentCoroutineContext().isActive && generation == playbackGeneration.get()) delay(40)
            if (!currentCoroutineContext().isActive || generation != playbackGeneration.get()) break
            val read = input.read(buffer)
            if (read < 0) break
            val pcm = frameBuffer.consume(buffer, read)
            if (pcm.isNotEmpty()) {
                val processedPcm = equalizer.process(pcm)
                audioLine.write(processedPcm, 0, processedPcm.size)
                totalBytes += pcm.size
                _position.value = startSeconds + totalBytes / (SAMPLE_RATE.toDouble() * FRAME_SIZE)
                val bucket = (_position.value / SESSION_SAVE_INTERVAL_SECONDS).toLong()
                if (bucket != persistedBucket) {
                    persistedBucket = bucket
                    saveQueueSession()
                }
            }
        }
        return totalBytes
    }

    private fun playAfterCompletion() {
        if (endOfTrackSleepLatch.consume()) {
            _state.value = State.IDLE
            _position.value = _duration.value
            saveQueueSession()
            return
        }
        val next = playbackQueue.moveNext(automatic = true)
        publishQueue()
        if (next == null) {
            _state.value = State.IDLE
            _position.value = _duration.value
        } else {
            scope.launch { startPlayback(next, 0.0) }
        }
    }

    fun playPause() {
        when (_state.value) {
            State.PLAYING -> {
                paused = true
                line?.stop()
                _state.value = State.PAUSED
                saveQueueSession()
            }
            State.PAUSED -> {
                paused = false
                line?.start()
                _state.value = State.PLAYING
            }
            State.IDLE -> playbackQueue.current?.let { startPlayback(it, _position.value.takeIf { p -> p < _duration.value } ?: 0.0) }
            State.ERROR -> retryPlayback()
            State.BUFFERING -> Unit
        }
    }

    fun seekTo(seconds: Float) {
        val item = playbackQueue.current ?: return
        val target = PlaybackSeekPolicy.normalize(seconds.toDouble(), _duration.value)
        startPlayback(item, target)
    }

    fun stop() {
        closeActivePlayback()
        playbackGeneration.incrementAndGet()
        paused = false
        _position.value = 0.0
        streamCache.clear()
        _playbackNotice.value = null
        _state.value = State.IDLE
        saveQueueSession()
    }

    fun retryPlayback() {
        val item = playbackQueue.current ?: return
        val resumePosition = _position.value.takeIf { it in 0.0..<_duration.value } ?: 0.0
        startPlayback(item, resumePosition)
    }

    fun dismissPlaybackNotice() {
        _playbackNotice.value = null
    }

    fun skipNext() {
        playbackQueue.moveNext()?.let { startPlayback(it, 0.0) }
        publishQueue()
    }

    fun skipPrevious() {
        when (playbackQueue.previousAction(_position.value)) {
            PreviousAction.RESTART_CURRENT -> playbackQueue.current?.let { startPlayback(it, 0.0) }
            PreviousAction.MOVE_PREVIOUS -> playbackQueue.movePrevious()?.let { startPlayback(it, 0.0) }
        }
        publishQueue()
    }

    fun selectQueueItem(index: Int) {
        playbackQueue.select(index)?.let { startPlayback(it, 0.0) }
        publishQueue()
    }

    fun removeQueueItem(index: Int) {
        val activeId = playbackQueue.current?.videoId
        playbackQueue.removeAt(index)
        publishQueue()
        if (activeId != playbackQueue.current?.videoId) playbackQueue.current?.let { startPlayback(it, 0.0) }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        playbackQueue.move(fromIndex, toIndex)
        publishQueue()
    }

    fun playQueueItemNext(index: Int) {
        playbackQueue.playNext(index)
        publishQueue()
    }

    fun clearUpcoming() {
        playbackQueue.clearUpcoming()
        publishQueue()
    }

    fun setShuffle(enabled: Boolean) {
        playbackQueue.setShuffle(enabled)
        _shuffle.value = playbackQueue.shuffled
        publishQueue()
    }

    fun cycleRepeatMode() {
        setRepeatMode(
            when (playbackQueue.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
            },
        )
    }

    fun setRepeatMode(mode: RepeatMode) {
        playbackQueue.repeatMode = mode
        _repeatMode.value = mode
    }

    fun setVolume(value: Int) {
        appliedVolumePercent = value.coerceIn(0, 100)
        _volumePercent.value = appliedVolumePercent
        line?.let(::applyVolume)
    }

    fun setEqualizer(configuration: EqualizerConfiguration) {
        equalizer.update(configuration)
    }

    fun setStopAfterCurrent(enabled: Boolean) {
        endOfTrackSleepLatch.enabled = enabled
    }

    fun availableAudioOutputs(): List<AudioOutputDevice> = runCatching(audioOutputProvider::devices)
        .getOrElse { listOf(AudioOutputDevice(AudioOutputDevice.SYSTEM_DEFAULT_ID, "System default")) }

    fun setAudioOutput(deviceId: String) {
        val resolved = AudioOutputSelection.resolve(deviceId, availableAudioOutputs())
        val previous = selectedAudioOutputId
        if (previous == resolved) return
        selectedAudioOutputId = resolved
        _audioOutputId.value = resolved
        if (AudioOutputChangePolicy.shouldReconnect(_state.value, previous, resolved)) {
            playbackQueue.current?.let { startPlayback(it, _position.value) }
        }
    }

    private fun applyVolume(audioLine: SourceDataLine) {
        if (!audioLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) return
        val control = audioLine.getControl(FloatControl.Type.MASTER_GAIN) as FloatControl
        val gain = if (appliedVolumePercent == 0) {
            control.minimum
        } else {
            (20f * log10(appliedVolumePercent / 100f)).coerceIn(control.minimum, control.maximum)
        }
        control.value = gain
    }

    private fun publishQueue() {
        _queueItems.value = playbackQueue.items
        _queueIndex.value = playbackQueue.currentIndex
        saveQueueSession()
    }

    private fun restoreQueueSession(snapshot: QueueSessionSnapshot) {
        val items = snapshot.items.map(QueueSessionItem::toQueueItem)
        playbackQueue.restore(items, snapshot.currentIndex, snapshot.shuffled)
        playbackQueue.repeatMode = snapshot.repeatMode
        _shuffle.value = snapshot.shuffled
        _repeatMode.value = snapshot.repeatMode
        _queueItems.value = items
        _queueIndex.value = playbackQueue.currentIndex
        playbackQueue.current?.let { item ->
            _currentSong.value = NowPlaying(item, item.title, item.artist, item.thumbnailUrl)
            _duration.value = item.duration.toDouble().coerceAtLeast(0.0)
            _position.value = snapshot.positionSeconds.coerceIn(0.0, _duration.value.takeIf { it > 0.0 } ?: Double.MAX_VALUE)
        }
    }

    private fun saveQueueSession() {
        val items = playbackQueue.items
        queueSessionStore?.save(
            QueueSessionSnapshot(
                items = items.map(QueueItem::toSessionItem),
                currentIndex = playbackQueue.currentIndex,
                positionSeconds = _position.value,
                shuffled = playbackQueue.shuffled,
                repeatMode = playbackQueue.repeatMode,
            ),
        )
    }

    private fun closeActivePlayback() {
        job?.cancel()
        job = null
        process?.destroyForcibly()
        process = null
        line?.close()
        line = null
    }

    fun dispose() {
        saveQueueSession()
        closeActivePlayback()
        playbackGeneration.incrementAndGet()
        scope.cancel()
    }

    private companion object {
        const val SAMPLE_RATE = 44_100
        const val CHANNELS = 2
        const val FRAME_SIZE = 4
        const val SESSION_SAVE_INTERVAL_SECONDS = 5.0
    }
}

private fun resolveFfmpegPath(): String? {
    DesktopMusicPlayer::class.java.getResourceAsStream("/ffmpeg.exe")?.use { resource ->
        val temp = File(System.getProperty("java.io.tmpdir"), "hikalist_ffmpeg.exe")
        if (!temp.exists() || temp.length() == 0L) {
            temp.outputStream().use(resource::copyTo)
            temp.setExecutable(true)
        }
        return temp.absolutePath
    }

    val lookup = Runtime.getRuntime().exec(arrayOf("where", "ffmpeg"))
    return if (lookup.waitFor() == 0) {
        lookup.inputStream.bufferedReader().readLines().firstOrNull()
    } else {
        null
    }
}
