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
    const val PROJECT_URL = "https://typdrsbcuvsfluziudsb.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_RYOxNL02Kr4gn18hKjudAQ_ylsN5Y8K"
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
