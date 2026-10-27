package com.nuvio.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.nuvio.app.desktop.DesktopPaths
import com.nuvio.app.desktop.DesktopWindowState

fun main() = application {
    val state = rememberWindowState(width = 1280.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = DesktopPaths.appName,
    ) {
        LaunchedEffect(window) {
            window.addWindowFocusListener(object : java.awt.event.WindowFocusListener {
                override fun windowGainedFocus(e: java.awt.event.WindowEvent?) { DesktopWindowState.focused.value = true }
                override fun windowLostFocus(e: java.awt.event.WindowEvent?) { DesktopWindowState.focused.value = false }
            })
        }
        App()
    }
}
