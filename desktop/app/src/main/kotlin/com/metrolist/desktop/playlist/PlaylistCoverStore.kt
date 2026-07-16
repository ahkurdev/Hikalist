package com.metrolist.desktop.playlist

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

class PlaylistCoverStore(
    private val coverDirectory: File = File(System.getProperty("user.home"), ".metrolist/covers"),
) {
    fun save(playlistId: String, source: File): File {
        val original = requireNotNull(ImageIO.read(source)) { "The selected file is not a supported image." }
        val output = squareCover(original)
        val safeId = playlistId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(80).ifBlank { "playlist" }

        coverDirectory.mkdirs()
        val destination = coverDirectory.resolve("$safeId-${System.currentTimeMillis()}.jpg").canonicalFile
        require(destination.toPath().startsWith(coverDirectory.canonicalFile.toPath())) { "Invalid cover location." }

        val writer = ImageIO.getImageWritersByFormatName("jpeg").asSequence().first()
        ImageIO.createImageOutputStream(destination).use { stream ->
            writer.output = stream
            val parameters = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = 0.82f
            }
            writer.write(null, IIOImage(output, null, null), parameters)
        }
        writer.dispose()
        return destination
    }

    fun deleteIfManaged(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path).canonicalFile
            val managedDirectory = coverDirectory.canonicalFile
            if (file.toPath().startsWith(managedDirectory.toPath()) && file.isFile) {
                file.delete()
            }
        }
    }

    private fun squareCover(source: BufferedImage): BufferedImage {
        val cropSize = minOf(source.width, source.height)
        val cropX = (source.width - cropSize) / 2
        val cropY = (source.height - cropSize) / 2
        return BufferedImage(COVER_SIZE, COVER_SIZE, BufferedImage.TYPE_INT_RGB).also { destination ->
            destination.createGraphics().use { graphics ->
                graphics.color = Color(35, 24, 22)
                graphics.fillRect(0, 0, COVER_SIZE, COVER_SIZE)
                graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                graphics.drawImage(
                    source,
                    0,
                    0,
                    COVER_SIZE,
                    COVER_SIZE,
                    cropX,
                    cropY,
                    cropX + cropSize,
                    cropY + cropSize,
                    null,
                )
            }
        }
    }

    private companion object {
        const val COVER_SIZE = 512
    }
}

private inline fun <T : java.awt.Graphics2D, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        dispose()
    }
