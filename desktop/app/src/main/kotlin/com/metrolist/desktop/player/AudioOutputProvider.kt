package com.metrolist.desktop.player

import java.security.MessageDigest
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.SourceDataLine

data class AudioOutputDevice(val id: String, val displayName: String) {
    companion object {
        const val SYSTEM_DEFAULT_ID = "system-default"

        fun stableId(name: String, vendor: String, description: String, version: String): String {
            val source = listOf(name, vendor, description, version).joinToString("\u0000")
            return MessageDigest.getInstance("SHA-256")
                .digest(source.toByteArray())
                .take(12)
                .joinToString("") { "%02x".format(it) }
        }
    }
}

object AudioOutputSelection {
    fun resolve(storedId: String?, devices: List<AudioOutputDevice>): String =
        storedId?.takeIf { id -> devices.any { it.id == id } } ?: AudioOutputDevice.SYSTEM_DEFAULT_ID
}

object AudioOutputChangePolicy {
    fun shouldReconnect(state: DesktopMusicPlayer.State, previousId: String, nextId: String): Boolean =
        previousId != nextId && state in setOf(DesktopMusicPlayer.State.PLAYING, DesktopMusicPlayer.State.BUFFERING)
}

interface AudioOutputProvider {
    fun devices(): List<AudioOutputDevice>
    fun openLine(format: AudioFormat, deviceId: String): SourceDataLine
}

class JavaSoundAudioOutputProvider : AudioOutputProvider {
    override fun devices(): List<AudioOutputDevice> {
        val format = AudioFormat(44_100f, 16, 2, true, false)
        val lineInfo = DataLine.Info(SourceDataLine::class.java, format)
        val outputs = AudioSystem.getMixerInfo().mapNotNull { info ->
            val mixer = runCatching { AudioSystem.getMixer(info) }.getOrNull() ?: return@mapNotNull null
            if (!mixer.isLineSupported(lineInfo)) return@mapNotNull null
            AudioOutputDevice(
                id = AudioOutputDevice.stableId(info.name, info.vendor, info.description, info.version),
                displayName = info.name.takeIf(String::isNotBlank) ?: info.description,
            )
        }.distinctBy(AudioOutputDevice::id)
        return listOf(AudioOutputDevice(AudioOutputDevice.SYSTEM_DEFAULT_ID, "System default")) + outputs
    }

    override fun openLine(format: AudioFormat, deviceId: String): SourceDataLine {
        val info = DataLine.Info(SourceDataLine::class.java, format)
        if (deviceId == AudioOutputDevice.SYSTEM_DEFAULT_ID) {
            return AudioSystem.getLine(info) as SourceDataLine
        }
        val mixerInfo = AudioSystem.getMixerInfo().firstOrNull { candidate ->
            AudioOutputDevice.stableId(
                candidate.name,
                candidate.vendor,
                candidate.description,
                candidate.version,
            ) == deviceId
        }
        val mixer = mixerInfo?.let(AudioSystem::getMixer)
        return if (mixer != null && mixer.isLineSupported(info)) {
            mixer.getLine(info) as SourceDataLine
        } else {
            AudioSystem.getLine(info) as SourceDataLine
        }
    }
}
