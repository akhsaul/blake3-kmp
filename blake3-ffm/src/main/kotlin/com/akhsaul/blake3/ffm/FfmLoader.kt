package com.akhsaul.blake3.ffm

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale.US
import java.util.concurrent.atomic.AtomicBoolean

private val loaded = AtomicBoolean(false)

@Suppress("UnsafeDynamicallyLoadedCode")
internal fun loadNativeLibrary() {
    if (loaded.getAndSet(true)) return

    val osName = System.getProperty("os.name").lowercase(US)
    val osArch = System.getProperty("os.arch").lowercase(US)

    val libName =
        when {
            osName.contains("windows") -> "blake3-kmp.dll"
            osName.contains("linux") -> "libblake3-kmp.so"
            osName.contains("mac") -> "libblake3-kmp.dylib"
            else -> error("Unsupported OS: $osName (arch=$osArch)")
        }

    val candidates =
        listOf(
            "/jni/$osArch/$libName",
            if (osArch == "amd64") "/jni/x86_64/$libName" else "/jni/amd64/$libName",
        )

    val inputStream =
        candidates.firstNotNullOfOrNull { path ->
            Blake3::class.java.getResourceAsStream(path)
        }

    if (inputStream == null) {
        try {
            System.loadLibrary("blake3-kmp")
            return
        } catch (_: Throwable) {
            error("Could not load native library $libName for os=$osName arch=$osArch. Tried resources $candidates")
        }
    }

    val tempFile = Files.createTempFile("blake3-ffm", null)
    tempFile.toFile().deleteOnExit()

    inputStream.use { stream ->
        Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
    }

    System.load(tempFile.toAbsolutePath().toString())
}
