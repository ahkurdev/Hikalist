package com.metrolist.desktop.auth

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

enum class DesktopAuthMode(val queryValue: String) {
    LOGIN("login"),
    REGISTER("register"),
}

data class DesktopAuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

fun DesktopAuthTokens.isIssuedBy(projectUrl: String): Boolean = runCatching {
    val payloadPart = accessToken.split('.').getOrNull(1) ?: return@runCatching false
    val payload = String(Base64.getUrlDecoder().decode(payloadPart), Charsets.UTF_8)
    val issuer = Regex("\"iss\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
        ?: return@runCatching false
    issuer.startsWith(projectUrl)
}.getOrDefault(false)

object DesktopAuthCallbackParser {
    private const val MAX_CALLBACK_LENGTH = 16 * 1024

    fun parse(body: String, expectedState: String): DesktopAuthTokens? {
        if (body.length > MAX_CALLBACK_LENGTH) return null
        val fields = runCatching {
            body.split('&').mapNotNull { field ->
                val separator = field.indexOf('=')
                if (separator <= 0) return@mapNotNull null
                decode(field.substring(0, separator)) to decode(field.substring(separator + 1))
            }.groupBy({ it.first }, { it.second })
        }.getOrNull() ?: return null

        val state = fields.singleValue("state") ?: return null
        if (!MessageDigest.isEqual(state.toByteArray(), expectedState.toByteArray())) return null
        val accessToken = fields.singleValue("access_token")?.takeIf(String::isNotBlank) ?: return null
        val refreshToken = fields.singleValue("refresh_token")?.takeIf(String::isNotBlank) ?: return null
        return DesktopAuthTokens(accessToken, refreshToken)
    }

    private fun Map<String, List<String>>.singleValue(name: String): String? =
        this[name]?.singleOrNull()

    private fun decode(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8)
}

object DesktopAuthLaunchUrl {
    fun build(
        accountUrl: String,
        port: Int,
        state: String,
        mode: DesktopAuthMode,
    ): String {
        require(port in 1024..65_535) { "Desktop callback port is invalid" }
        require(state.matches(Regex("[A-Za-z0-9_-]{32,128}"))) { "Desktop auth state is invalid" }
        val baseUri = URI(accountUrl)
        require(baseUri.scheme == "https" && baseUri.host != null && baseUri.userInfo == null) {
            "Account URL must be a secure HTTPS origin"
        }
        val base = accountUrl.trimEnd('/')
        return "$base/?desktop_port=$port&state=${encode(state)}&mode=${mode.queryValue}"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8)
}
