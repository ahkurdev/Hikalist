package com.metrolist.desktop.auth

import com.metrolist.desktop.sync.HikalistSupabase
import com.metrolist.desktop.sync.HikalistSupabaseConfig
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import java.awt.Desktop
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.io.File
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

enum class DesktopAccountStatus {
    INITIALIZING,
    SIGNED_OUT,
    WAITING_FOR_BROWSER,
    SIGNED_IN,
    ERROR,
}

data class DesktopAccountState(
    val status: DesktopAccountStatus = DesktopAccountStatus.INITIALIZING,
    val userId: String? = null,
    val email: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val message: String? = null,
) {
    companion object {
        fun signedIn(
            email: String,
            profile: DesktopAccountProfile,
            userId: String? = null,
            message: String = "Your playlists sync automatically when you are online",
        ) = DesktopAccountState(
            status = DesktopAccountStatus.SIGNED_IN,
            userId = userId,
            email = email,
            username = profile.username,
            avatarUrl = profile.avatarUrl,
            message = message,
        )
    }
}

class DesktopAuthManager(
    private val client: SupabaseClient = HikalistSupabase.client,
    private val accountUrl: String = System.getenv("HIKALIST_ACCOUNT_URL")
        ?.takeIf(String::isNotBlank)
        ?: DEFAULT_ACCOUNT_URL,
    private val profiles: DesktopAccountProfileRepository = DesktopAccountProfileRepository(
        SupabaseDesktopProfileDataSource(client),
    ),
    private val profileEditor: DesktopProfileEditor = DesktopProfileEditor(
        profiles = profiles,
        avatarStore = CloudinaryDesktopAvatarStore(),
    ),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val mutableState = MutableStateFlow(DesktopAccountState())
    private var browserAuthJob: Job? = null

    val state: StateFlow<DesktopAccountState> = mutableState.asStateFlow()

    init {
        scope.launch {
            runCatching {
                client.auth.awaitInitialization()
                publishCurrentSession()
            }.onFailure { error ->
                mutableState.value = DesktopAccountState(
                    DesktopAccountStatus.ERROR,
                    message = error.userMessage("Could not load your Hikalist account"),
                )
            }
        }
    }

    fun begin(mode: DesktopAuthMode) {
        browserAuthJob?.cancel()
        browserAuthJob = scope.launch {
            runBrowserAuth(mode)
        }
    }

    fun signOut() {
        browserAuthJob?.cancel()
        scope.launch {
            runCatching { client.auth.signOut() }
                .onSuccess { mutableState.value = DesktopAccountState(DesktopAccountStatus.SIGNED_OUT) }
                .onFailure { error ->
                    mutableState.value = DesktopAccountState(
                        DesktopAccountStatus.ERROR,
                        email = client.auth.currentSessionOrNull()?.user?.email,
                        message = error.userMessage("Could not sign out"),
                    )
                }
        }
    }

    fun updateProfile(username: String, avatarFile: File?) {
        val current = mutableState.value
        val userId = current.userId ?: return
        val email = current.email ?: return
        scope.launch {
            mutableState.value = current.copy(message = "Saving profile…")
            runCatching { profileEditor.update(userId, username, avatarFile) }
                .onSuccess { profile ->
                    mutableState.value = DesktopAccountState.signedIn(
                        email = email,
                        profile = profile,
                        userId = userId,
                        message = "Profile updated. Changes sync automatically.",
                    )
                }
                .onFailure { error ->
                    mutableState.value = current.copy(
                        message = error.userMessage("Could not update profile"),
                    )
                }
        }
    }

    fun close() {
        browserAuthJob?.cancel()
        scope.cancel()
    }

    private suspend fun runBrowserAuth(mode: DesktopAuthMode) {
        val callback = CompletableDeferred<AuthCallbackRequest>()
        val stateToken = createStateToken()
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "hikalist-auth-callback").apply { isDaemon = true }
        }
        val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            this.executor = executor
            createContext(CALLBACK_PATH) { exchange -> handleCallback(exchange, stateToken, callback) }
        }

        try {
            server.start()
            val launchUrl = DesktopAuthLaunchUrl.build(
                accountUrl = accountUrl,
                port = server.address.port,
                state = stateToken,
                mode = mode,
            )
            mutableState.value = DesktopAccountState(
                DesktopAccountStatus.WAITING_FOR_BROWSER,
                message = "Complete ${mode.queryValue} in your browser",
            )
            openBrowser(launchUrl)

            val request = withTimeout(AUTH_TIMEOUT) { callback.await() }
            runCatching {
                client.auth.importAuthToken(
                    accessToken = request.tokens.accessToken,
                    refreshToken = request.tokens.refreshToken,
                    retrieveUser = true,
                    autoRefresh = true,
                )
                publishCurrentSession()
            }.onSuccess {
                request.exchange.respond(
                    200,
                    "Hikalist is connected. You can close this tab and return to the desktop app.",
                )
            }.onFailure { error ->
                request.exchange.respond(401, "The Hikalist session could not be verified. Please try again.")
                throw error
            }
        } catch (error: Throwable) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            mutableState.value = DesktopAccountState(
                DesktopAccountStatus.ERROR,
                email = client.auth.currentSessionOrNull()?.user?.email,
                message = error.userMessage("Could not connect the browser to Hikalist"),
            )
        } finally {
            server.stop(0)
            executor.shutdownNow()
        }
    }

    private fun handleCallback(
        exchange: HttpExchange,
        expectedState: String,
        callback: CompletableDeferred<AuthCallbackRequest>,
    ) {
        if (exchange.requestMethod != "POST" || exchange.remoteAddress.address?.isLoopbackAddress != true) {
            exchange.respond(405, "This callback only accepts a local POST request.")
            return
        }
        val bodyBytes = exchange.requestBody.use { it.readNBytes(MAX_CALLBACK_BYTES + 1) }
        if (bodyBytes.size > MAX_CALLBACK_BYTES) {
            exchange.respond(413, "The callback request is too large.")
            return
        }
        val tokens = DesktopAuthCallbackParser.parse(bodyBytes.toString(Charsets.UTF_8), expectedState)
        if (tokens == null) {
            exchange.respond(400, "The callback request is invalid or expired.")
            return
        }
        if (!tokens.isIssuedBy(HikalistSupabaseConfig.PROJECT_URL)) {
            exchange.respond(
                400,
                "This web session belongs to a different Hikalist server. Refresh the web page and sign in again.",
            )
            return
        }
        if (!callback.complete(AuthCallbackRequest(tokens, exchange))) {
            exchange.respond(409, "This callback has already been used.")
        }
    }

    private suspend fun publishCurrentSession() {
        val session = client.auth.currentSessionOrNull()
        mutableState.value = if (session == null) {
            DesktopAccountState(DesktopAccountStatus.SIGNED_OUT)
        } else {
            val userId = session.user?.id ?: error("The saved Hikalist session has no user ID")
            val email = session.user?.email ?: error("The saved Hikalist session has no email")
            val profile = try {
                profiles.loadOrCreate(userId, email)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                DesktopAccountProfileRepository.fallback(email)
            }
            DesktopAccountState.signedIn(email, profile, userId)
        }
    }

    private fun openBrowser(url: String) {
        check(Desktop.isDesktopSupported()) { "This desktop cannot open a browser" }
        val desktop = Desktop.getDesktop()
        check(desktop.isSupported(Desktop.Action.BROWSE)) { "This desktop cannot open a browser" }
        desktop.browse(URI(url))
    }

    private fun createStateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private data class AuthCallbackRequest(
        val tokens: DesktopAuthTokens,
        val exchange: HttpExchange,
    )

    private companion object {
        const val DEFAULT_ACCOUNT_URL = "https://hikalist.ahkur.my.id"
        const val CALLBACK_PATH = "/auth/callback"
        const val MAX_CALLBACK_BYTES = 16 * 1024
        val AUTH_TIMEOUT = 5.minutes
    }
}

private fun HttpExchange.respond(status: Int, message: String) {
    val escaped = message
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    val body = """
        <!doctype html>
        <html lang="en">
          <head><meta charset="utf-8"><meta name="viewport" content="width=device-width"></head>
          <body style="margin:0;background:#160f10;color:#f9eeee;font:16px system-ui;display:grid;place-items:center;min-height:100vh">
            <main style="max-width:520px;padding:36px;text-align:center"><h1>Hikalist</h1><p>$escaped</p></main>
          </body>
        </html>
    """.trimIndent().toByteArray()
    responseHeaders.set("Content-Type", "text/html; charset=utf-8")
    responseHeaders.set("Cache-Control", "no-store")
    responseHeaders.set("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'")
    sendResponseHeaders(status, body.size.toLong())
    responseBody.use { it.write(body) }
}

private fun Throwable.userMessage(fallback: String): String = message
    ?.takeIf(String::isNotBlank)
    ?.let { "$fallback: $it" }
    ?: fallback
