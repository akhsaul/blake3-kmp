package com.akhsaul.blake3

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Blake3Test {

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testEmptyStringHash() {
        val hash = Blake3.hash(ByteArray(0))
        val hexString = hash.toHexString()
        assertEquals("af1349b9f5f9a1a6a0404dea36dcc9499bcb25c9adc112b7cc9a93cae41f3262", hexString)
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testStreamingUpdateMatchesSingleShot() {
        val part1 = "hello ".encodeToByteArray()
        val part2 = "world".encodeToByteArray()
        val combined = "hello world".encodeToByteArray()

        val singleHash = Blake3.hash(combined).toHexString()

        val streamingHash = Blake3Hasher().use { hasher ->
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
}
