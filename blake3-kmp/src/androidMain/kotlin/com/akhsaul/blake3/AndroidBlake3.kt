package com.akhsaul.blake3

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale.US
import java.util.concurrent.atomic.AtomicBoolean

private val loaded = AtomicBoolean(false)

@Suppress("UnsafeDynamicallyLoadedCode")
internal actual fun loadNativeLibrary() {
    if (loaded.getAndSet(true)) return

    try {
        System.loadLibrary("blake3-kmp")
        return
    } catch (_: UnsatisfiedLinkError) {
        // Not running on Android (e.g. a desktop JVM executing host unit tests
        // against the Android variant): fall through to desktop libraries
        // bundled under /native-host/ resources. Consumers exclude
        // /native-host/* when packaging Android apps.
    }

    val osName = (System.getProperty("os.name") ?: "").lowercase(US)
    val osArch = (System.getProperty("os.arch") ?: "").lowercase(US)

    val libName =
        when {
            osName.contains("windows") -> "blake3-kmp.dll"
            osName.contains("linux") -> "libblake3-kmp.so"
            osName.contains("mac") -> "libblake3-kmp.dylib"
            else -> error("Unsupported OS: $osName (arch=$osArch)")
        }

    // AGP strips *.so from classes.jar Java resources, so the Linux
    // library is stored as libblake3-kmp.so.bin. System.load works with any
    // file extension since it loads by full temp-file path.
    val candidates =
        listOf(
            "/jni/$osArch/$libName",
            if (osArch == "amd64") "/jni/x86_64/$libName" else "/jni/amd64/$libName",
            "/native-host/$osArch/$libName",
            "/native-host/$osArch/$libName.bin",
            if (osArch == "amd64") "/native-host/x86_64/$libName" else "/native-host/amd64/$libName",
            if (osArch == "amd64") "/native-host/x86_64/$libName.bin" else "/native-host/amd64/$libName.bin",
        )

    val inputStream =
        candidates.firstNotNullOfOrNull { path ->
            JniBlake3::class.java.getResourceAsStream(path)
        }

    if (inputStream == null) {
        error("Could not load native library $libName for os=$osName arch=$osArch. Tried resources $candidates")
    }

    val tempFile = Files.createTempFile("blake3-kmp", null)
    tempFile.toFile().deleteOnExit()

    inputStream.use { stream ->
        Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
    }

    System.load(tempFile.toAbsolutePath().toString())
}
