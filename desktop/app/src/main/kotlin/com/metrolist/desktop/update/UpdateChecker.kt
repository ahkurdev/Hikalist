package com.metrolist.desktop.update

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AppVersion(version: String) : Comparable<AppVersion> {
    private val components = version.trim().removePrefix("v").substringBefore('-')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }
        .let { parts -> List(maxOf(3, parts.size)) { index -> parts.getOrElse(index) { 0 } } }

    val value: String = components.joinToString(".")

    override fun compareTo(other: AppVersion): Int {
        repeat(maxOf(components.size, other.components.size)) { index ->
            val comparison = components.getOrElse(index) { 0 }.compareTo(other.components.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }

    override fun equals(other: Any?): Boolean = other is AppVersion && compareTo(other) == 0
    override fun hashCode(): Int = components.hashCode()
    override fun toString(): String = value

    companion object {
        val CURRENT = AppVersion("1.0.2")
    }
}

data class ReleaseInfo(val tagName: String, val pageUrl: String)

fun interface ReleaseSource {
    suspend fun latest(): ReleaseInfo
}

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: AppVersion, val pageUrl: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}

class UpdateChecker(
    private val currentVersion: AppVersion = AppVersion.CURRENT,
    private val releaseSource: ReleaseSource = GitHubReleaseSource(),
) {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    suspend fun check(): UpdateState {
        _state.value = UpdateState.Checking
        return runCatching {
            val release = releaseSource.latest()
            val latest = AppVersion(release.tagName)
            if (latest > currentVersion) UpdateState.Available(latest, release.pageUrl) else UpdateState.UpToDate
        }.getOrElse { error ->
            UpdateState.Failed(error.message ?: "Could not check for updates")
        }.also { _state.value = it }
    }
}

class GitHubReleaseSource(
    private val repository: String = "Allan4u/Hikalist",
    private val client: HttpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
) : ReleaseSource {
    override suspend fun latest(): ReleaseInfo = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.github.com/repos/$repository/releases/latest"))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Hikalist-Desktop")
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        require(response.statusCode() in 200..299) {
            if (response.statusCode() == 404) "No Hikalist release has been published yet" else "GitHub returned ${response.statusCode()}"
        }
        val json = Json.parseToJsonElement(response.body()).jsonObject
        ReleaseInfo(
            tagName = requireNotNull(json["tag_name"]?.jsonPrimitive?.content),
            pageUrl = requireNotNull(json["html_url"]?.jsonPrimitive?.content),
        )
    }
}
