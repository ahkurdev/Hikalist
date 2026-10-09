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
        ?: "https://menfessdarmajaya.my.id/hikalist"
    val PUBLISHABLE_KEY = System.getenv("HIKALIST_SUPABASE_KEY")
        ?.takeIf(String::isNotBlank)
        ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJyb2xlIjoiYW5vbiIsImlzcyI6Imh0dHBzOi8vbWVuZmVzc2Rhcm1hamF5YS5teS5pZC9oaWthbGlzdCIsImlhdCI6MTc5MTQwMDAwMCwiZXhwIjoyMTA2NzYwMDAwfQ.7H6sf6icSbkVL4jckeeh1mCWeYWMdNEcllcZ3V_ckBM"
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
