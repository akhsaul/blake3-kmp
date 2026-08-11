package com.akhsaul.blake3.ffm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Blake3Test {
    @Test
    fun testCustomOutputLength() {
        val data = "BLAKE3 - JVM Custom Output Length".encodeToByteArray()

        val hash16 = Blake3.hash(data, outLen = 16)
        val actualHash16 = hash16.toHexString()
        val expected16 = "ac430da494d860fbd273123c8bc60048"
        assertEquals(16, hash16.size)
        assertEquals(expected16, actualHash16)

        val hash64 = Blake3.hash(data, outLen = 40)
        val actualHash64 = hash64.toHexString()
        val expected64 = "ac430da494d860fbd273123c8bc6004871fed12b6f755c08f6d4c14ac204b8eb37cafe4d492dd822"
        assertEquals(40, hash64.size)
        assertEquals(expected64, actualHash64)
    }

    @Test
    fun testKeyedHash() {
        val key = "3bc471e75b7fbc99531eb3fbe146aabc".toByteArray()
        val data = "BLAKE3 - JVM Keyed Hash Test".encodeToByteArray()

        val hash = Blake3.hash(data)
        val keyedHash = Blake3.keyedHash(key, data)
        val actualDefaultResult = hash.toHexString()
        val actualKeyedResult = keyedHash.toHexString()

        val expectedKeyedResult = "7695bd397344ce67a45dedbc19cd553fc9b60b3f5b1e420802d438c4254990b6"
        val expectedDefaultResult = "df459e8215d093073f4cb4c51bde2bdafeaa7ef125fbce12cb1b50e82bd6a045"

        assertEquals(32, keyedHash.size)
        assertTrue(!keyedHash.contentEquals(hash))
        assertEquals(expectedKeyedResult, actualKeyedResult)
        assertEquals(expectedDefaultResult, actualDefaultResult)
    }

    @Test
    fun testStreamHashing() {
        val data = "Hello World via Blake3StreamFfm".encodeToByteArray()
        val oneShot = Blake3.hash(data)

        Blake3Stream().use { hasher ->
            hasher.update(data)
            val streamHash = hasher.finalize()
            assertTrue(oneShot.contentEquals(streamHash))
        }
    }
}
