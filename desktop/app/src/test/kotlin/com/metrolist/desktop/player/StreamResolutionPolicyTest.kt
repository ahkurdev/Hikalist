package com.metrolist.desktop.player

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StreamResolutionPolicyTest {
    @Test
    fun `select skips a candidate whose stream URL is rejected`() = runBlocking {
        val attemptedClients = mutableListOf<String>()

        val selected = StreamResolutionPolicy.select(
            candidates = listOf("ANDROID_VR", "VISIONOS"),
            resolve = { client ->
                attemptedClients += client
                when (client) {
                    "ANDROID_VR" -> "https://cdn.example/rejected"
                    else -> "https://cdn.example/playable"
                }
            },
            validate = { _, url -> url.endsWith("playable") },
        )

        assertEquals(listOf("ANDROID_VR", "VISIONOS"), attemptedClients)
        assertEquals("VISIONOS", selected?.candidate)
        assertEquals("https://cdn.example/playable", selected?.value)
    }

    @Test
    fun `select returns null after every stream URL is rejected`() = runBlocking {
        val selected = StreamResolutionPolicy.select(
            candidates = listOf("VISIONOS", "ANDROID_VR"),
            resolve = { client -> "https://cdn.example/$client" },
            validate = { _, _ -> false },
        )

        assertNull(selected)
    }
}
