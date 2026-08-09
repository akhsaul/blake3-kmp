package com.akhsaul.blake3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Blake3Test {
    @Test
    fun testEmptyStringHash() {
        val hash = Blake3.hash(ByteArray(0))
        val hexString = hash.toHexString()
        assertEquals("af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262", hexString)
    }

    @Test
    fun testStreamingUpdateMatchesSingleShot() {
        val part1 = "hello ".encodeToByteArray()
        val part2 = "world".encodeToByteArray()
        val combined = "hello world".encodeToByteArray()

        val singleHash = Blake3.hash(combined).toHexString()

        val streamingHash =
            Blake3Hasher().use { hasher ->
                hasher.update(part1)
                hasher.update(part2)
                hasher.finalize().toHexString()
            }

        assertEquals(singleHash, streamingHash)
    }

    @Test
    fun testCustomOutputLength() {
        val data = "BLAKE3 KMP".encodeToByteArray()

        val hash16 = Blake3.hash(data, outLen = 16)
        assertEquals(16, hash16.size)

        val hash64 = Blake3.hash(data, outLen = 64)
        assertEquals(64, hash64.size)
    }

    @Test
    fun testKeyedHash() {
        val key = ByteArray(32) { it.toByte() }
        val data = "Keyed Hash Test".encodeToByteArray()

        val keyedResult = Blake3.keyedHash(key, data)
        val defaultResult = Blake3.hash(data)

        assertEquals(32, keyedResult.size)
        assertTrue(!keyedResult.contentEquals(defaultResult))
    }

    @Test
    fun test1MBFileChannelMap() {
        val size = 1 * 1024 * 1024 // 1 MB
        val pattern = "BLAKE3_1MB_TEST_DATA_".encodeToByteArray()
        val inMemoryData = ByteArray(size) { i -> pattern[i % pattern.size] }

        val singleShotHash = Blake3.hash(inMemoryData).toHexString()

        val tempFile = java.io.File.createTempFile("blake3_1mb_test", ".tmp")
        tempFile.deleteOnExit()
        try {
            java.io.RandomAccessFile(tempFile, "rw").use { raf ->
                raf.write(inMemoryData)
            }

            val streamingHash =
                java.io.RandomAccessFile(tempFile, "r").use { raf ->
                    val channel = raf.channel
                    val mappedBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, channel.size())
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
