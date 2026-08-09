package com.akhsaul.blake3

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

        val actualKeyedResult = Blake3.keyedHash(key, data)
        val actualDefaultResult = Blake3.hash(data)

        val expectedKeyedResult = "7695bd397344ce67a45dedbc19cd553fc9b60b3f5b1e420802d438c4254990b6"
        val expectedDefaultResult = "df459e8215d093073f4cb4c51bde2bdafeaa7ef125fbce12cb1b50e82bd6a045"

        assertEquals(32, actualKeyedResult.size)
        assertTrue(!actualKeyedResult.contentEquals(actualDefaultResult))
        assertEquals(expectedKeyedResult, actualKeyedResult.toHexString())
        assertEquals(expectedDefaultResult, actualDefaultResult.toHexString())
    }

    @Test
    fun testPrecomputed2MBBinaryFileChecksum() {
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

        val tempFile = java.io.File.createTempFile("blake3_test_bin", ".tmp")
        tempFile.deleteOnExit()
        try {
            tempFile.outputStream().use { out ->
                inputStream.copyTo(out)
            }

            val actualHash =
                java.io.RandomAccessFile(tempFile, "r").use { raf ->
                    val channel = raf.channel
                    val mappedBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, channel.size())
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
