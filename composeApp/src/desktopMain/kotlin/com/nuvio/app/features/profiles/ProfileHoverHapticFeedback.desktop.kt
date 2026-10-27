package com.nuvio.app.features.profiles

// No haptics on a PC.
internal actual object ProfileHoverHapticFeedback {
    actual fun prepare() = Unit
    actual fun perform() = Unit
    actual fun release() = Unit
}
