package com.metrolist.desktop.player

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class DesktopPlaybackProbeTest {
    @Test
    fun `real anonymous streams decode to non-silent frame-aligned PCM`() = runBlocking {
        assumeTrue(
            "Set METROLIST_NETWORK_TESTS=1 to run the real YouTube playback probe",
            System.getenv("METROLIST_NETWORK_TESTS") == "1",
        )

        val ffmpegResource = javaClass.getResourceAsStream("/ffmpeg.exe")
        assertNotNull("Bundled ffmpeg.exe is missing", ffmpegResource)

        val ffmpeg = File.createTempFile("metrolist-playback-probe-", ".exe")
        ffmpegResource!!.use { input -> ffmpeg.outputStream().use(input::copyTo) }
        ffmpeg.setExecutable(true)

        try {
            listOf(
                "9YPX7WjPDp8",
                "h8EbYGvhu6I",
                "D0fzGl640wE",
            ).forEach { videoId ->
                val playback = DesktopYTPlayerUtils.resolveStream(videoId).getOrThrow()
                val stderr = File.createTempFile("metrolist-playback-probe-", ".log")
                try {
                    val process = ProcessBuilder(
                        ffmpeg.absolutePath,
                        "-hide_banner", "-loglevel", "error",
                        "-user_agent", playback.userAgent,
                        "-i", playback.streamUrl,
                        "-t", "2",
                        "-vn",
                        "-f", "s16le",
                        "-acodec", "pcm_s16le",
                        "-ar", "44100",
                        "-ac", "2",
                        "pipe:1",
                    ).redirectError(stderr).start()

                    val pcm = process.inputStream.readBytes()
                    val exitCode = process.waitFor()
                    val errorText = stderr.readText()

                    assertEquals("FFmpeg failed for $videoId: $errorText", 0, exitCode)
                    assertTrue("Less than one second of PCM for $videoId", pcm.size >= 44_100 * 4)
                    assertEquals("PCM is not stereo-frame aligned for $videoId", 0, pcm.size % 4)
                    assertTrue("PCM appears silent for $videoId", pcm.any { it.toInt() != 0 })
                } finally {
                    stderr.delete()
                }
            }
        } finally {
            ffmpeg.delete()
        }
    }
}
