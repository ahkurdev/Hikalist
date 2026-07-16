package com.metrolist.desktop.auth

import com.metrolist.desktop.sync.HikalistSupabase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import java.io.File
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class DesktopAccountProfile(
    val username: String,
    val avatarUrl: String?,
)

data class DesktopProfileInsert(
    val userId: String,
    val username: String,
    val email: String,
)

interface DesktopProfileDataSource {
    suspend fun find(userId: String): DesktopAccountProfile?

    suspend fun insert(profile: DesktopProfileInsert): DesktopAccountProfile

    suspend fun update(userId: String, profile: DesktopAccountProfile): DesktopAccountProfile
}

class DesktopAccountProfileRepository(
    private val dataSource: DesktopProfileDataSource = SupabaseDesktopProfileDataSource(),
) {
    suspend fun loadOrCreate(userId: String, email: String): DesktopAccountProfile {
        dataSource.find(userId)?.let { return it }
        val normalizedEmail = email.trim().lowercase()
        val username = fallback(email).username
        return dataSource.insert(
            DesktopProfileInsert(
                userId = userId,
                username = username,
                email = normalizedEmail,
            ),
        )
    }

    suspend fun find(userId: String): DesktopAccountProfile? = dataSource.find(userId)

    suspend fun update(userId: String, profile: DesktopAccountProfile): DesktopAccountProfile =
        dataSource.update(userId, profile)

    companion object {
        fun fallback(email: String): DesktopAccountProfile = DesktopAccountProfile(
            username = email.substringBefore('@').trim().take(MAX_USERNAME_LENGTH)
                .ifBlank { DEFAULT_USERNAME },
            avatarUrl = null,
        )

        const val MAX_USERNAME_LENGTH = 40
        const val DEFAULT_USERNAME = "Hikalist listener"
    }
}

internal class SupabaseDesktopProfileDataSource(
    private val client: SupabaseClient = HikalistSupabase.client,
) : DesktopProfileDataSource {
    override suspend fun find(userId: String): DesktopAccountProfile? = client
        .from(PROFILE_TABLE)
        .select(columns = Columns.list("username", "avatar_url")) {
            filter { eq("user_id", userId) }
            limit(1)
        }
        .decodeList<SupabaseDesktopProfile>()
        .firstOrNull()
        ?.toAccountProfile()

    override suspend fun insert(profile: DesktopProfileInsert): DesktopAccountProfile {
        client.from(PROFILE_TABLE).insert(
            SupabaseDesktopProfileInsert(
                userId = profile.userId,
                username = profile.username,
                email = profile.email,
            ),
        )
        return DesktopAccountProfile(profile.username, avatarUrl = null)
    }

    override suspend fun update(userId: String, profile: DesktopAccountProfile): DesktopAccountProfile {
        client.from(PROFILE_TABLE).update(
            SupabaseDesktopProfileUpdate(profile.username, profile.avatarUrl),
        ) {
            filter { eq("user_id", userId) }
        }
        return profile
    }

    private companion object {
        const val PROFILE_TABLE = "hika_profiles"
    }
}

@Serializable
private data class SupabaseDesktopProfile(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
) {
    fun toAccountProfile() = DesktopAccountProfile(username, avatarUrl)
}

@Serializable
private data class SupabaseDesktopProfileInsert(
    @SerialName("user_id") val userId: String,
    val username: String,
    val email: String,
)

@Serializable
private data class SupabaseDesktopProfileUpdate(
    val username: String,
    @SerialName("avatar_url") val avatarUrl: String?,
)

interface DesktopAvatarStore {
    suspend fun upload(userId: String, file: File): String
}

class DesktopProfileEditor(
    private val profiles: DesktopAccountProfileRepository = DesktopAccountProfileRepository(),
    private val avatarStore: DesktopAvatarStore = SupabaseDesktopAvatarStore(),
) {
    suspend fun update(userId: String, username: String, avatarFile: File?): DesktopAccountProfile {
        val normalizedUsername = username.trim()
        require(normalizedUsername.isNotEmpty()) { "Username cannot be empty" }
        require(normalizedUsername.length <= DesktopAccountProfileRepository.MAX_USERNAME_LENGTH) {
            "Username can contain at most ${DesktopAccountProfileRepository.MAX_USERNAME_LENGTH} characters"
        }
        val current = profiles.find(userId) ?: throw IllegalStateException("Hikalist profile was not found")
        val avatarUrl = avatarFile?.let { avatarStore.upload(userId, it) } ?: current.avatarUrl
        return profiles.update(userId, DesktopAccountProfile(normalizedUsername, avatarUrl))
    }
}

internal class SupabaseDesktopAvatarStore(
    private val client: SupabaseClient = HikalistSupabase.client,
) : DesktopAvatarStore {
    override suspend fun upload(userId: String, file: File): String {
        require(file.isFile) { "Choose a valid avatar image" }
        require(file.length() <= MAX_AVATAR_BYTES) { "Avatar must be 2 MB or smaller" }
        val extension = file.extension.lowercase().let { if (it == "jpeg") "jpg" else it }
        require(extension in AVATAR_EXTENSIONS) { "Avatar must be JPG, PNG, or WebP" }
        val objectPath = "$userId/avatar.$extension"
        client.storage[AVATAR_BUCKET].upload(objectPath, file.readBytes()) { upsert = true }
        return client.storage[AVATAR_BUCKET].publicUrl(objectPath)
    }

    private companion object {
        const val AVATAR_BUCKET = "avatars"
        const val MAX_AVATAR_BYTES = 2L * 1024L * 1024L
        val AVATAR_EXTENSIONS = setOf("jpg", "png", "webp")
    }
}
