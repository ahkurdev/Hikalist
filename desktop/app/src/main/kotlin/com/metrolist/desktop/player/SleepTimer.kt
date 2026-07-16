package com.metrolist.desktop.player

import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SleepTimerOption(val durationSeconds: Long?) {
    OFF(null),
    MINUTES_15(15 * 60L),
    MINUTES_30(30 * 60L),
    MINUTES_45(45 * 60L),
    MINUTES_60(60 * 60L),
    END_OF_TRACK(null),
}

data class SleepTimerState(
    val option: SleepTimerOption = SleepTimerOption.OFF,
    val remainingSeconds: Long? = null,
)

class EndOfTrackSleepLatch {
    @Volatile var enabled: Boolean = false

    fun consume(): Boolean = synchronized(this) {
        if (!enabled) return@synchronized false
        enabled = false
        true
    }
}

class SleepTimerController(
    private val stopPlayback: () -> Unit,
    private val setStopAfterCurrent: (Boolean) -> Unit,
) : Closeable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null
    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    fun select(option: SleepTimerOption) {
        countdownJob?.cancel()
        countdownJob = null
        setStopAfterCurrent(false)

        when (option) {
            SleepTimerOption.OFF -> _state.value = SleepTimerState()
            SleepTimerOption.END_OF_TRACK -> {
                setStopAfterCurrent(true)
                _state.value = SleepTimerState(option)
            }
            else -> startCountdown(option, requireNotNull(option.durationSeconds))
        }
    }

    private fun startCountdown(option: SleepTimerOption, durationSeconds: Long) {
        val deadlineMillis = System.currentTimeMillis() + durationSeconds * 1_000L
        _state.value = SleepTimerState(option, durationSeconds)
        countdownJob = scope.launch {
            while (true) {
                val remaining = ((deadlineMillis - System.currentTimeMillis() + 999L) / 1_000L).coerceAtLeast(0L)
                _state.value = SleepTimerState(option, remaining)
                if (remaining == 0L) break
                delay(1_000L)
            }
            stopPlayback()
            _state.value = SleepTimerState()
        }
    }

    override fun close() {
        countdownJob?.cancel()
        setStopAfterCurrent(false)
        scope.cancel()
    }
}
