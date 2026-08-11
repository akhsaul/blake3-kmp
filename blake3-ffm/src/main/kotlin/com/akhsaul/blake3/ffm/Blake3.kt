package com.akhsaul.blake3.ffm

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

object Blake3 {
    const val KEY_LEN: Int = 32
    const val OUT_LEN: Int = 32

    @Suppress("unused")
    const val BLOCK_LEN: Int = 64

    @Suppress("unused")
    const val CHUNK_LEN: Int = 1024

    @Suppress("DuplicatedCode")
    fun hash(
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray {
        require(outLen > 0) { "Output length must be positive" }
        require(data.isNotEmpty()) { "Input must be non-empty" }
        val result = ByteArray(outLen)
        val arena = Arena.ofConfined()
        arena.use { arena ->
            val hasherSegment = arena.allocate(FfmBlake3.hasherSize)
            FfmBlake3.blake3_hasher_init_handle.invokeExact(hasherSegment)

            val inputSegment = arena.allocate(data.size.toLong())
            MemorySegment.copy(data, 0, inputSegment, ValueLayout.JAVA_BYTE, 0L, data.size)
            FfmBlake3.blake3_hasher_update_handle.invokeExact(hasherSegment, inputSegment, data.size.toLong())

            val outputSegment = arena.allocate(outLen.toLong())
            FfmBlake3.blake3_hasher_finalize_handle.invokeExact(hasherSegment, outputSegment, outLen.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, result, 0, outLen)
        }
        return result
    }

    @Suppress("unused")
    fun hashHex(
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String = hash(data, outLen).toHexString(format)

    @Suppress("DuplicatedCode")
    fun keyedHash(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray {
        require(key.size == KEY_LEN) { "Key size must be $KEY_LEN bytes" }
        require(outLen > 0) { "Output length must be positive" }
        require(data.isNotEmpty()) { "Input must be non-empty" }
        val result = ByteArray(outLen)
        val arena = Arena.ofConfined()
        arena.use { arena ->
            val hasherSegment = arena.allocate(FfmBlake3.hasherSize)
            val keySegment = arena.allocate(32L)
            MemorySegment.copy(key, 0, keySegment, ValueLayout.JAVA_BYTE, 0L, 32)
            FfmBlake3.blake3_hasher_init_keyed_handle.invokeExact(hasherSegment, keySegment)

            val inputSegment = arena.allocate(data.size.toLong())
            MemorySegment.copy(data, 0, inputSegment, ValueLayout.JAVA_BYTE, 0L, data.size)
            FfmBlake3.blake3_hasher_update_handle.invokeExact(hasherSegment, inputSegment, data.size.toLong())

            val outputSegment = arena.allocate(outLen.toLong())
            FfmBlake3.blake3_hasher_finalize_handle.invokeExact(hasherSegment, outputSegment, outLen.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, result, 0, outLen)
        }
        return result
    }

    @Suppress("unused")
    fun keyedHashHex(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String = keyedHash(key, data, outLen).toHexString(format)
}
