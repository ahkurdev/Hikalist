package com.metrolist.desktop.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesktopAuthCallbackTest {
    @Test
    fun `callback parser accepts matching state and decodes tokens`() {
        val state = "A".repeat(43)

        val result = DesktopAuthCallbackParser.parse(
            body = "state=$state&access_token=access%2Btoken&refresh_token=refresh%2Ftoken",
            expectedState = state,
        )

        assertEquals(DesktopAuthTokens("access+token", "refresh/token"), result)
    }

    @Test
    fun `callback parser rejects mismatched state and missing tokens`() {
        val state = "B".repeat(43)

        assertNull(
            DesktopAuthCallbackParser.parse(
                body = "state=${"C".repeat(43)}&access_token=access&refresh_token=refresh",
                expectedState = state,
            ),
        )
        assertNull(DesktopAuthCallbackParser.parse("state=$state&access_token=access", state))
    }

    @Test
    fun `callback parser rejects malformed form encoding without crashing`() {
        val state = "B".repeat(43)

        assertNull(
            DesktopAuthCallbackParser.parse(
                body = "state=$state&access_token=%ZZ&refresh_token=refresh",
                expectedState = state,
            ),
        )
    }

    @Test
    fun `launch URL only targets the configured account site and loopback port`() {
        val state = "D".repeat(43)

        val url = DesktopAuthLaunchUrl.build(
            accountUrl = "https://web-hikalist.vercel.app/",
            port = 49_152,
            state = state,
            mode = DesktopAuthMode.REGISTER,
        )

        assertEquals(
            "https://web-hikalist.vercel.app/?desktop_port=49152&state=$state&mode=register",
            url,
        )
    }
}
