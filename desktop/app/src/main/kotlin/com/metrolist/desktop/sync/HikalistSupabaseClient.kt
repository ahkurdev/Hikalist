package com.metrolist.desktop.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlin.time.Duration.Companion.seconds

object HikalistSupabaseConfig {
    val PROJECT_URL = System.getenv("HIKALIST_SUPABASE_URL")
        ?.takeIf(String::isNotBlank)
        ?: "https://api-hikalist.ahkur.my.id"
    val PUBLISHABLE_KEY = System.getenv("HIKALIST_SUPABASE_KEY")
        ?.takeIf(String::isNotBlank)
        ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6Imh0dHBzOi8vYXBpLWhpa2FsaXN0LmFoa3VyLm15LmlkIiwiaWF0IjoxNzkxNDAwMDAwLCJleHAiOjIxMDY3NjAwMDB9.Xg9wI1QPMGoOFp1ecXwJipbTkm_cwLdpS-aIEI0pD_o"
}

object HikalistSupabase {
    val client: SupabaseClient by lazy(::createHikalistSupabaseClient)
}

internal fun createHikalistSupabaseClient(): SupabaseClient = createSupabaseClient(
    supabaseUrl = HikalistSupabaseConfig.PROJECT_URL,
    supabaseKey = HikalistSupabaseConfig.PUBLISHABLE_KEY,
) {
    install(Auth) {
        sessionManager = SettingsSessionManager()
        alwaysAutoRefresh = true
        autoLoadFromStorage = true
        autoSaveToStorage = true
    }
    install(Postgrest)
    install(Realtime) {
        reconnectDelay = 3.seconds
    }
    install(Storage)
}
