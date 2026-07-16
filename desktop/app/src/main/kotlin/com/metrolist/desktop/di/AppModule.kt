package com.metrolist.desktop.di

import com.metrolist.desktop.db.repository.DataRepository
import com.metrolist.desktop.preferences.DesktopPreferences
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

private val appModule = module {
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { DataRepository() }
    single { DesktopPreferences }
}

fun initKoin() {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(appModule)
        }
    }
}
