package com.akhsaul.blake3

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class Blake3AndroidDeviceTest {
    @Test
    fun testAndroidNativeLibraryLoading() {
        val hash = Blake3.hash("Hello Android BLAKE3".encodeToByteArray())
        assertEquals(32, hash.size)
        assertTrue(hash.toHexString().isNotEmpty())
    }

    @Test
    fun testAndroid1MBFileChannelMap() {
        val size = 1 * 1024 * 1024 // 1 MB
        val pattern = "BLAKE3_ANDROID_DEVICE_TEST_".encodeToByteArray()
        val inMemoryData = ByteArray(size) { i -> pattern[i % pattern.size] }

        val singleShotHash = Blake3.hash(inMemoryData).toHexString()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val tempFile = File(context.cacheDir, "blake3_android_1mb.tmp")
        try {
            RandomAccessFile(tempFile, "rw").use { raf ->
                raf.write(inMemoryData)
            }

            val streamingHash =
                RandomAccessFile(tempFile, "r").use { raf ->
                    val channel = raf.channel
                    val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                    Blake3Hasher().use { hasher ->
                        val chunkSize = 64 * 1024 // 64 KB
                        val chunk = ByteArray(chunkSize)
                        while (mappedBuffer.hasRemaining()) {
                            val toRead = minOf(chunkSize, mappedBuffer.remaining())
                            mappedBuffer.get(chunk, 0, toRead)
                            hasher.update(chunk, 0, toRead)
                        }
                        hasher.finalize().toHexString()
                    }
                }

            assertEquals(singleShotHash, streamingHash)
        } finally {
            tempFile.delete()
        }
    }
}
