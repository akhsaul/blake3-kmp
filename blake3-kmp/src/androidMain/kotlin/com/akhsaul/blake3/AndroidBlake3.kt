package com.akhsaul.blake3

import java.util.concurrent.atomic.AtomicBoolean

private val loaded = AtomicBoolean(false)

internal actual fun loadNativeLibrary() {
    if (loaded.getAndSet(true)) return
    System.loadLibrary("blake3-kmp")
}
