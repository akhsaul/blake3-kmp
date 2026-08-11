package com.akhsaul.blake3.ffm

import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Blake3Test {
    @Test
    fun testCustomOutputLength() {
        val data = "BLAKE3-FFM - JVM Custom Output Length".encodeToByteArray()

        val hash16 = Blake3.hash(data, outLen = 16)
        val actualHash16 = hash16.toHexString()
        val expected16 = "c7293ba6a952b99d9d28bcc67c477580"
        assertEquals(16, hash16.size)
        assertEquals(expected16, actualHash16)

        val hash64 = Blake3.hash(data, outLen = 40)
        val actualHash64 = hash64.toHexString()
        val expected64 = "c7293ba6a952b99d9d28bcc67c477580fcd47c821f9402ac3bba554390b2703111574d257e7d5650"
        assertEquals(40, hash64.size)
        assertEquals(expected64, actualHash64)
    }

    @Test
    fun testKeyedHash() {
        val key = "3bc471e75b7fbc99531eb3fbe146aabc".toByteArray()
        val data = "BLAKE3-FFM - JVM Keyed Hash Test".encodeToByteArray()

        val hash = Blake3.hash(data)
        val keyedHash = Blake3.keyedHash(key, data)
        val actualDefaultResult = hash.toHexString()
        val actualKeyedResult = keyedHash.toHexString()

        val expectedKeyedResult = "a623e5d98d186112aec1b06c4a126a672a8c33ad5d469b312b520fcd5a9c90cd"
        val expectedDefaultResult = "4710365addea9fd22524d6c5669345294ffa6ba26da6bc8e6dc1fe016d2c114c"

        assertEquals(32, keyedHash.size)
        assertTrue(!keyedHash.contentEquals(hash))
        assertEquals(expectedKeyedResult, actualKeyedResult)
        assertEquals(expectedDefaultResult, actualDefaultResult)
    }

    @Test
    fun testPrecomputedBinaryFileChecksum() {
        val checksumStream =
            Blake3Test::class.java.getResourceAsStream("/bin/test.bin.blake3")
                ?: return
        val expectedChecksum =
            checksumStream.bufferedReader().use { reader ->
                reader.readLine()?.substringBefore(' ')?.trim()
            } ?: return

        val inputStream =
            Blake3Test::class.java.getResourceAsStream("/bin/test.bin")
                ?: return

        val tempFile = File.createTempFile("blake3_test_bin", ".tmp")
        tempFile.deleteOnExit()
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            val actualHash =
                RandomAccessFile(tempFile, "r").use { raf ->
                    val channel = raf.channel
                    val mappedBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, channel.size())
                    Blake3Stream().use { hasher ->
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

            val fullData = tempFile.readBytes()
            val actualOneShotHash = Blake3.hash(fullData).toHexString()
            assertEquals(expectedChecksum, actualOneShotHash)
        } finally {
            tempFile.delete()
        }
    }
}
