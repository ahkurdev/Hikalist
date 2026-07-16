package com.metrolist.desktop.player

import kotlin.random.Random
import kotlinx.serialization.Serializable

@Serializable
enum class RepeatMode { OFF, ALL, ONE }

enum class PreviousAction { RESTART_CURRENT, MOVE_PREVIOUS }

class PlaybackQueue(randomSeed: Int? = null) {
    private val random = randomSeed?.let(::Random) ?: Random.Default
    private var sourceItems = emptyList<DesktopMusicPlayer.QueueItem>()
    private var queueItems = emptyList<DesktopMusicPlayer.QueueItem>()
    private var selectedIndex = -1

    var repeatMode: RepeatMode = RepeatMode.ALL
    var shuffled: Boolean = false
        private set

    val items: List<DesktopMusicPlayer.QueueItem>
        get() = queueItems

    val current: DesktopMusicPlayer.QueueItem?
        get() = queueItems.getOrNull(selectedIndex)

    val currentIndex: Int
        get() = selectedIndex

    fun setSource(items: List<DesktopMusicPlayer.QueueItem>, selectedIndex: Int = 0) {
        sourceItems = items.toList()
        queueItems = sourceItems
        this.selectedIndex = if (items.isEmpty()) -1 else selectedIndex.coerceIn(items.indices)
        shuffled = false
    }

    fun restore(items: List<DesktopMusicPlayer.QueueItem>, selectedIndex: Int, shuffled: Boolean) {
        sourceItems = items.toList()
        queueItems = items.toList()
        this.selectedIndex = if (items.isEmpty()) -1 else selectedIndex.coerceIn(items.indices)
        this.shuffled = shuffled
    }

    fun setShuffle(enabled: Boolean) {
        if (enabled == shuffled || queueItems.isEmpty()) return
        val activeId = current?.videoId
        queueItems = if (enabled) {
            val active = current
            val rest = sourceItems.filterNot { it.videoId == activeId }.shuffled(random)
            listOfNotNull(active) + rest
        } else {
            sourceItems
        }
        selectedIndex = queueItems.indexOfFirst { it.videoId == activeId }.coerceAtLeast(0)
        shuffled = enabled
    }

    fun moveNext(automatic: Boolean = false): DesktopMusicPlayer.QueueItem? {
        if (queueItems.isEmpty()) return null
        if (automatic && repeatMode == RepeatMode.ONE) return current
        if (selectedIndex < queueItems.lastIndex) {
            selectedIndex += 1
            return current
        }
        if (repeatMode == RepeatMode.ALL) {
            selectedIndex = 0
            return current
        }
        return null
    }

    fun movePrevious(): DesktopMusicPlayer.QueueItem? {
        if (queueItems.isEmpty()) return null
        if (selectedIndex > 0) {
            selectedIndex -= 1
            return current
        }
        if (repeatMode == RepeatMode.ALL) {
            selectedIndex = queueItems.lastIndex
            return current
        }
        return current
    }

    fun previousAction(positionSeconds: Double): PreviousAction =
        if (positionSeconds > 5.0) PreviousAction.RESTART_CURRENT else PreviousAction.MOVE_PREVIOUS

    fun select(index: Int): DesktopMusicPlayer.QueueItem? {
        if (index !in queueItems.indices) return null
        selectedIndex = index
        return current
    }

    fun removeAt(index: Int): DesktopMusicPlayer.QueueItem? {
        if (index !in queueItems.indices) return current
        val removed = queueItems[index]
        queueItems = queueItems.toMutableList().also { it.removeAt(index) }
        sourceItems = sourceItems.filterNot { it.videoId == removed.videoId }
        selectedIndex = when {
            queueItems.isEmpty() -> -1
            index < selectedIndex -> selectedIndex - 1
            selectedIndex > queueItems.lastIndex -> queueItems.lastIndex
            else -> selectedIndex
        }
        return current
    }

    fun move(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in queueItems.indices || toIndex !in queueItems.indices || fromIndex == toIndex) return
        val activeId = current?.videoId
        queueItems = queueItems.toMutableList().also { list ->
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
        }
        if (!shuffled) sourceItems = queueItems
        selectedIndex = queueItems.indexOfFirst { it.videoId == activeId }
    }

    fun playNext(index: Int) {
        if (index !in queueItems.indices || index == selectedIndex) return
        val activeId = current?.videoId ?: return
        queueItems = queueItems.toMutableList().also { list ->
            val item = list.removeAt(index)
            val activeIndex = list.indexOfFirst { it.videoId == activeId }
            list.add((activeIndex + 1).coerceAtMost(list.size), item)
        }
        if (!shuffled) sourceItems = queueItems
        selectedIndex = queueItems.indexOfFirst { it.videoId == activeId }
    }

    fun clearUpcoming() {
        val active = current
        queueItems = listOfNotNull(active)
        sourceItems = queueItems
        selectedIndex = if (active == null) -1 else 0
    }
}
