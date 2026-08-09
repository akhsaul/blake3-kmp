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
        val expected16 = "2ba0098206cc93e537c79f9d8270fcb6"
        assertEquals(expected16, hash16.toHexString())

        val hash64 = Blake3.hash(data, outLen = 40)
        assertEquals(40, hash64.size)
        // from b3sum binary
        val expected64 = "2ba0098206cc93e537c79f9d8270fcb6798df6ea138683944cf62562bef776b71aac0d57f9c285a7"
        assertEquals(expected64, hash64.toHexString())
    }

    @Test
    fun testKeyedHash() {
        val key = "3bc471e75b7fbc99531eb3fbe146aabc".toByteArray()
        val data = "BLAKE3 - Android Keyed Hash Test".encodeToByteArray()

        val actualKeyedResult = Blake3.keyedHash(key, data)
        val actualDefaultResult = Blake3.hash(data)

        val expectedKeyedResult = "00403b84e3df714b4527067ef3e122658a8882e0c8665191812992fc5fb63f80"
        val expectedDefaultResult = "b1e7e12454e0df7331c0aa8830ff1b67c7b00f5b8f333a8c0211f8c2f8e40490"

        assertEquals(32, actualKeyedResult.size)
        assertTrue(!actualKeyedResult.contentEquals(actualDefaultResult))
        assertEquals(expectedKeyedResult, actualKeyedResult.toHexString())
        assertEquals(expectedDefaultResult, actualDefaultResult.toHexString())
    }
}
