package com.nuvio.app.desktop

import kotlinx.coroutines.flow.MutableStateFlow

/** Window facts the shared code asks the platform about, kept up to date by the desktop main(). */
object DesktopWindowState {
    val focused = MutableStateFlow(true)
}
