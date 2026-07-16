package com.metrolist.desktop.auth

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DesktopAccountProfileRepositoryTest {
    @Test
    fun `signed in account state exposes the synced profile`() {
        val state = DesktopAccountState.signedIn(
            email = "finalformkyu@gmail.com",
            profile = DesktopAccountProfile(
                username = "Allan4u",
                avatarUrl = "https://example.com/avatar.webp",
            ),
        )

        assertEquals(DesktopAccountStatus.SIGNED_IN, state.status)
        assertEquals("finalformkyu@gmail.com", state.email)
        assertEquals("Allan4u", state.username)
        assertEquals("https://example.com/avatar.webp", state.avatarUrl)
    }

    @Test
    fun `existing profile is returned without overwriting it`() = runBlocking {
        val existing = DesktopAccountProfile(username = "Allan4u", avatarUrl = "https://example.com/avatar.webp")
        val dataSource = FakeDesktopProfileDataSource(existing)
        val repository = DesktopAccountProfileRepository(dataSource)

        val result = repository.loadOrCreate("user-1", "finalformkyu@gmail.com")

        assertEquals(existing, result)
        assertNull(dataSource.insertedProfile)
    }

    @Test
    fun `missing profile is created from the account email without upsert`() = runBlocking {
        val dataSource = FakeDesktopProfileDataSource(profile = null)
        val repository = DesktopAccountProfileRepository(dataSource)

        val result = repository.loadOrCreate("user-1", "FinalFormKyu@gmail.com")

        assertEquals(DesktopAccountProfile(username = "FinalFormKyu", avatarUrl = null), result)
        assertEquals(
            DesktopProfileInsert(
                userId = "user-1",
                username = "FinalFormKyu",
                email = "finalformkyu@gmail.com",
            ),
            dataSource.insertedProfile,
        )
    }

    @Test
    fun `profile editor uploads avatar and persists normalized username`() = runBlocking {
        val dataSource = FakeDesktopProfileDataSource(
            DesktopAccountProfile(username = "Old name", avatarUrl = "old-avatar"),
        )
        val avatarStore = FakeAvatarStore("https://cdn.example/new.webp")
        val editor = DesktopProfileEditor(DesktopAccountProfileRepository(dataSource), avatarStore)

        val updated = editor.update("user-1", "  Allan4u  ", File("avatar.webp"))

        assertEquals(DesktopAccountProfile("Allan4u", "https://cdn.example/new.webp"), updated)
        assertEquals(updated, dataSource.updatedProfile)
        assertEquals("user-1", avatarStore.uploadedUserId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `profile editor rejects blank username before uploading`() {
        runBlocking {
            val dataSource = FakeDesktopProfileDataSource(DesktopAccountProfile("Old", null))
            val avatarStore = FakeAvatarStore("unused")

            DesktopProfileEditor(DesktopAccountProfileRepository(dataSource), avatarStore)
                .update("user-1", "   ", File("avatar.webp"))
        }
    }
}

private class FakeDesktopProfileDataSource(
    private val profile: DesktopAccountProfile?,
) : DesktopProfileDataSource {
    var insertedProfile: DesktopProfileInsert? = null
    var updatedProfile: DesktopAccountProfile? = null

    override suspend fun find(userId: String): DesktopAccountProfile? = profile

    override suspend fun insert(profile: DesktopProfileInsert): DesktopAccountProfile {
        insertedProfile = profile
        return DesktopAccountProfile(profile.username, avatarUrl = null)
    }

    override suspend fun update(userId: String, profile: DesktopAccountProfile): DesktopAccountProfile {
        updatedProfile = profile
        return profile
    }
}

private class FakeAvatarStore(private val url: String) : DesktopAvatarStore {
    var uploadedUserId: String? = null

    override suspend fun upload(userId: String, file: File): String {
        uploadedUserId = userId
        return url
    }
}
