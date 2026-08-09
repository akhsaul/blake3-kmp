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
    fun testCustomOutputLength() {
        val data = "BLAKE3 - Android Custom Output Length".encodeToByteArray()

        val hash16 = Blake3.hash(data, outLen = 16)
        assertEquals(16, hash16.size)
        // from b3sum binary
        val expected16 = "5e6f0a85c495905898a4ec412b03086f"
        assertEquals(expected16, hash16.toHexString())

        val hash64 = Blake3.hash(data, outLen = 40)
        assertEquals(40, hash64.size)
        // from b3sum binary
        val expected64 = "5e6f0a85c495905898a4ec412b03086f8ca69aae673ac8e03a0b5970ae160b5cd72fe78ecba42977"
        assertEquals(expected64, hash64.toHexString())
    }

    @Test
    fun testKeyedHash() {
        val key = "3bc471e75b7fbc99531eb3fbe146aab".toByteArray()
        val data = "BLAKE3 - Android Keyed Hash Test".encodeToByteArray()

        val actualKeyedResult = Blake3.keyedHash(key, data)
        val actualDefaultResult = Blake3.hash(data)

        val expectedKeyedResult = "d385d034857999c69c1968757932edd39cac9f903593366a6d75dff2e1a33b42"
        val expectedDefaultResult = "d4aeaa3f9feb511feb7d7f6f6afce0272c8a9e38fadc881ae80c1ad05faf4ead"

        assertEquals(32, actualKeyedResult.size)
        assertTrue(!actualKeyedResult.contentEquals(actualDefaultResult))
        assertEquals(expectedKeyedResult, actualKeyedResult.toHexString())
        assertEquals(expectedDefaultResult, actualDefaultResult.toHexString())
    }

    @Test
    fun testAndroidPrecomputed2MBBinaryFileChecksum() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val checksumStream = context.assets.open("bin/test.bin.blake3")
        val expectedChecksum =
            checksumStream.bufferedReader().use { reader ->
                reader.readLine()?.substringBefore(' ')?.trim()
            } ?: error("Failed to read checksum from assets")

        val inputStream = context.assets.open("bin/test.bin")
        val tempFile = File(context.cacheDir, "blake3_android_2mb.tmp")
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            val actualHash =
                RandomAccessFile(tempFile, "r").use { raf ->
                    val channel = raf.channel
                    val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size())
                    Blake3Hasher().use { hasher ->
                        val chunkSize = 64 * 1024
                        val chunk = ByteArray(chunkSize)
                        while (mappedBuffer.hasRemaining()) {
                            val toRead = minOf(chunkSize, mappedBuffer.remaining())
                            mappedBuffer.get(chunk, 0, toRead)
                            hasher.update(chunk, 0, toRead)
                        }
                        hasher.finalize().toHexString()
                    }
                }

            assertEquals(expectedChecksum, actualHash)
        } finally {
            tempFile.delete()
        }
    }
}
