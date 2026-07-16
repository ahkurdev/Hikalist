package com.metrolist.desktop.sync

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class PlaylistSyncCoordinator(
    private val store: PlaylistSyncStore,
    private val remote: PlaylistSyncRemote,
    applyRemote: suspend (RemoteSyncBatch) -> Unit,
    private val pollInterval: Duration = 5.seconds,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val engine = PlaylistSyncEngine(store, remote, applyRemote)
    private val requests = Channel<Unit>(Channel.CONFLATED)
    private val mutableStatus = MutableStateFlow(
        PlaylistSyncStatus(pendingOperations = store.countPending()),
    )
    private var loopJob: Job? = null

    val status: StateFlow<PlaylistSyncStatus> = mutableStatus.asStateFlow()

    @Synchronized
    fun start() {
        if (loopJob?.isActive == true) return
        scope.launch {
            try {
                remote.subscribe { requestSync() }
            } catch (_: Throwable) {
                requestSync()
            }
        }
        loopJob = scope.launch {
            while (currentCoroutineContext().isActive) {
                runOnce()
                withTimeoutOrNull(pollInterval) { requests.receive() }
            }
        }
    }

    fun enqueue(operation: SyncOperation) {
        store.enqueue(operation)
        mutableStatus.value = mutableStatus.value.copy(
            pendingOperations = store.countPending(),
        )
        requestSync()
    }

    fun requestSync() {
        requests.trySend(Unit)
    }

    suspend fun runOnce(): SyncRunResult {
        mutableStatus.value = mutableStatus.value.copy(
            state = SyncConnectionState.SYNCING,
            pendingOperations = store.countPending(),
            message = null,
        )
        val result = engine.syncOnce()
        val pending = store.countPending()
        mutableStatus.value = when (result) {
            SyncRunResult.SYNCED -> PlaylistSyncStatus(
                state = SyncConnectionState.SYNCED,
                pendingOperations = pending,
                lastSyncedAtEpochSeconds = nowEpochSeconds(),
            )
            SyncRunResult.OFFLINE -> mutableStatus.value.copy(
                state = SyncConnectionState.OFFLINE,
                pendingOperations = pending,
                message = "Waiting for internet",
            )
            SyncRunResult.AUTH_REQUIRED -> mutableStatus.value.copy(
                state = SyncConnectionState.AUTH_REQUIRED,
                pendingOperations = pending,
                message = "Sign in to sync playlists",
            )
            SyncRunResult.ERROR -> mutableStatus.value.copy(
                state = SyncConnectionState.ERROR,
                pendingOperations = pending,
                message = "Sync will retry automatically",
            )
        }
        return result
    }

    suspend fun close() {
        scope.cancel()
        remote.close()
    }
}
