package com.metrolist.desktop.storage

import java.awt.Desktop
import java.io.File

data class StorageSnapshot(
    val downloadBytes: Long,
    val cacheBytes: Long,
    val downloadDirectory: File,
)

class StorageManager(
    private val downloadDirectory: File = File(System.getProperty("user.home"), ".metrolist/offline"),
    private val cacheDirectories: List<File> = listOf(
        File(System.getProperty("user.home"), ".metrolist/lyrics"),
        File(System.getProperty("java.io.tmpdir"), "hikalist-media-art"),
    ),
) {
    fun snapshot(): StorageSnapshot = StorageSnapshot(
        downloadBytes = directorySize(downloadDirectory),
        cacheBytes = cacheDirectories.sumOf(::directorySize),
        downloadDirectory = downloadDirectory,
    )

    fun clearCache() {
        cacheDirectories.forEach(::clearDirectoryContents)
    }

    fun clearDownloads() {
        clearDirectoryContents(downloadDirectory)
    }

    fun openDownloadDirectory(): Boolean = runCatching {
        downloadDirectory.mkdirs()
        if (!Desktop.isDesktopSupported()) return@runCatching false
        val desktop = Desktop.getDesktop()
        if (!desktop.isSupported(Desktop.Action.OPEN)) return@runCatching false
        desktop.open(downloadDirectory)
        true
    }.getOrDefault(false)

    private fun directorySize(directory: File): Long = runCatching {
        if (!directory.exists()) return@runCatching 0L
        directory.walkTopDown()
            .filter(File::isFile)
            .sumOf { file -> runCatching(file::length).getOrDefault(0L) }
    }.getOrDefault(0L)

    private fun clearDirectoryContents(directory: File) {
        runCatching {
            val root = directory.canonicalFile
            if (root.exists()) {
                root.listFiles().orEmpty().forEach { child ->
                    val canonicalChild = child.canonicalFile
                    require(canonicalChild != root && canonicalChild.toPath().startsWith(root.toPath())) {
                        "Refusing to delete outside the managed storage directory"
                    }
                    canonicalChild.walkBottomUp().forEach(File::delete)
                }
            }
            root.mkdirs()
        }
    }
}
