package com.metrolist.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.metrolist.desktop.di.initKoin
import com.metrolist.desktop.preferences.AppSettings
import com.metrolist.desktop.preferences.useDarkTheme
import com.metrolist.desktop.ui.AppLayout
import com.metrolist.desktop.ui.theme.MetrolistDesktopTheme
import com.metrolist.desktop.window.DesktopWindowController
import com.metrolist.desktop.window.HikalistWindowTitleBar

fun main() = application {
    initKoin()
    val windowState = rememberWindowState(
        position = WindowPosition.Aligned(Alignment.Center),
        size = DpSize(1024.dp, 600.dp),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Hikalist",
        state = windowState,
        undecorated = true,
    ) {
        val settings = remember { AppSettings() }
        val theme by settings.theme.collectAsState()
        val controller = remember(window, windowState) { DesktopWindowController(window, windowState) }
        DisposableEffect(controller) {
            controller.install()
            onDispose(controller::close)
        }
        LaunchedEffect(Unit) {
            window.minimumSize = java.awt.Dimension(900, 560)
        }
        MetrolistDesktopTheme(darkTheme = theme.useDarkTheme(isSystemInDarkTheme())) {
            Column(Modifier.fillMaxSize()) {
                HikalistWindowTitleBar(
                    controller = controller,
                    onClose = ::exitApplication,
                )
                Box(Modifier.weight(1f).fillMaxSize()) {
                    AppLayout(settings)
                }
            }
        }
    }
}
