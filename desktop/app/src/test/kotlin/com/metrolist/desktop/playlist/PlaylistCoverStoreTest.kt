package com.metrolist.desktop.playlist

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCoverStoreTest {
    @Test
    fun `saved cover is a safe compressed square image`() {
        val tempDirectory = Files.createTempDirectory("hikalist-cover-test").toFile()
        val source = tempDirectory.resolve("wide-source.png")
        val image = BufferedImage(900, 450, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().use { graphics ->
            graphics.color = Color(123, 74, 58)
            graphics.fillRect(0, 0, image.width, image.height)
        }
        ImageIO.write(image, "png", source)

        val coverDirectory = tempDirectory.resolve("covers")
        val saved = PlaylistCoverStore(coverDirectory).save("../unsafe playlist", source)
        val decoded = ImageIO.read(saved)

        assertTrue(saved.canonicalPath.startsWith(coverDirectory.canonicalPath))
        assertTrue(saved.name.endsWith(".jpg"))
        assertEquals(512, decoded.width)
        assertEquals(512, decoded.height)
        assertTrue(saved.length() < 250_000)
    }
}

private inline fun <T : java.awt.Graphics2D, R> T.use(block: (T) -> R): R =
    try {
        block(this)
    } finally {
        dispose()
    }
