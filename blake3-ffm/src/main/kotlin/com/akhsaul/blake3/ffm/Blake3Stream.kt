package com.akhsaul.blake3.ffm

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class Blake3Stream : AutoCloseable {
    private var arena: Arena? = Arena.ofShared()
    private var hasherSegment: MemorySegment? = null

    init {
        loadNativeLibrary()
    }

    constructor() {
        val currentArena = arena ?: error("Blake3Stream is closed")
        val segment = currentArena.allocate(FfmBlake3.hasherSize)
        FfmBlake3.blake3_hasher_init_handle.invokeExact(segment)
        hasherSegment = segment
    }

    @Suppress("unused")
    constructor(key: ByteArray) {
        require(key.size == Blake3.KEY_LEN) { "Key size must be ${Blake3.KEY_LEN} bytes" }
        val currentArena = arena ?: error("Blake3Stream is closed")
        val segment = currentArena.allocate(FfmBlake3.hasherSize)
        val keySegment = currentArena.allocate(32L)
        MemorySegment.copy(key, 0, keySegment, ValueLayout.JAVA_BYTE, 0L, 32)
        FfmBlake3.blake3_hasher_init_keyed_handle.invokeExact(segment, keySegment)
        hasherSegment = segment
    }

    fun update(
        input: ByteArray,
        offset: Int = 0,
        length: Int = input.size - offset,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= input.size) { "Index out of bounds" }
        val segment = hasherSegment ?: error("Blake3Stream is closed")

        val tempArena = Arena.ofConfined()
        tempArena.use { tempArena ->
            val inputSegment = tempArena.allocate(length.toLong())
            MemorySegment.copy(input, offset, inputSegment, ValueLayout.JAVA_BYTE, 0L, length)
            FfmBlake3.blake3_hasher_update_handle.invokeExact(segment, inputSegment, length.toLong())
            Unit
        }
    }

    fun finalize(
        out: ByteArray,
        offset: Int = 0,
        length: Int = out.size - offset,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= out.size) { "Index out of bounds" }
        val segment = hasherSegment ?: error("Blake3Stream is closed")

        val tempArena = Arena.ofConfined()
        tempArena.use { tempArena ->
            val outputSegment = tempArena.allocate(length.toLong())
            FfmBlake3.blake3_hasher_finalize_handle.invokeExact(segment, outputSegment, length.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, out, offset, length)
        }
    }

    fun finalize(outLen: Int = Blake3.OUT_LEN): ByteArray {
        val result = ByteArray(outLen)
        finalize(result, 0, outLen)
        return result
    }

    @Suppress("unused")
    fun reset() {
        val segment = hasherSegment ?: error("Blake3Stream is closed")
        FfmBlake3.blake3_hasher_reset_handle.invokeExact(segment)
    }

    override fun close() {
        val currentArena = arena
        if (currentArena != null) {
            hasherSegment = null
            arena = null
            try {
                currentArena.close()
            } catch (_: Throwable) {
            }
        }
    }
}
