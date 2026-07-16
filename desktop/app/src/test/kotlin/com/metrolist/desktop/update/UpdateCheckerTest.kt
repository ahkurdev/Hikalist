package com.metrolist.desktop.update

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun `semantic versions compare numeric components`() {
        assertTrue(AppVersion("1.10.0") > AppVersion("1.9.9"))
        assertTrue(AppVersion("2.0.0") > AppVersion("1.99.99"))
        assertEquals(AppVersion("1.2"), AppVersion("v1.2.0"))
    }

    @Test
    fun `newer release is reported with its page`() = runBlocking {
        val checker = UpdateChecker(
            currentVersion = AppVersion("1.0.0"),
            releaseSource = ReleaseSource {
                ReleaseInfo("v1.1.0", "https://github.com/Allan4u/Hikalist/releases/tag/v1.1.0")
            },
        )

        val state = checker.check()

        assertTrue(state is UpdateState.Available)
        assertEquals("1.1.0", (state as UpdateState.Available).version.value)
    }

    @Test
    fun `same release is up to date`() = runBlocking {
        val checker = UpdateChecker(AppVersion("1.0.0"), ReleaseSource { ReleaseInfo("v1.0.0", "url") })

        assertEquals(UpdateState.UpToDate, checker.check())
    }
}
