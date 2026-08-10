package com.akhsaul.blake3.ffm

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout

class Blake3StreamFfm : AutoCloseable {
    private var arena: Arena? = Arena.ofShared()
    private var hasherSegment: MemorySegment? = null

    init {
        loadFfmNativeLibrary()
    }

    constructor() {
        val currentArena = arena ?: error("Blake3StreamFfm is closed")
        val segment = currentArena.allocate(Blake3Ffm.hasherSize)
        Blake3Ffm.blake3_hasher_init_handle.invokeExact(segment)
        hasherSegment = segment
    }

    constructor(key: ByteArray) {
        require(key.size == Blake3Ffm.KEY_LEN) { "Key size must be ${Blake3Ffm.KEY_LEN} bytes" }
        val currentArena = arena ?: error("Blake3StreamFfm is closed")
        val segment = currentArena.allocate(Blake3Ffm.hasherSize)
        val keySegment = currentArena.allocate(32L)
        MemorySegment.copy(key, 0, keySegment, ValueLayout.JAVA_BYTE, 0L, 32)
        Blake3Ffm.blake3_hasher_init_keyed_handle.invokeExact(segment, keySegment)
        hasherSegment = segment
    }

    fun update(
        input: ByteArray,
        offset: Int = 0,
        length: Int = input.size - offset,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= input.size) { "Index out of bounds" }
        val segment = hasherSegment ?: error("Blake3StreamFfm is closed")

        val tempArena = Arena.ofConfined()
        try {
            val inputSegment = tempArena.allocate(length.toLong())
            MemorySegment.copy(input, offset, inputSegment, ValueLayout.JAVA_BYTE, 0L, length)
            Blake3Ffm.blake3_hasher_update_handle.invokeExact(segment, inputSegment, length.toLong())
        } finally {
            tempArena.close()
        }
    }

    fun finalize(
        out: ByteArray,
        offset: Int = 0,
        length: Int = out.size - offset,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= out.size) { "Index out of bounds" }
        val segment = hasherSegment ?: error("Blake3StreamFfm is closed")

        val tempArena = Arena.ofConfined()
        try {
            val outputSegment = tempArena.allocate(length.toLong())
            Blake3Ffm.blake3_hasher_finalize_handle.invokeExact(segment, outputSegment, length.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, out, offset, length)
        } finally {
            tempArena.close()
        }
    }

    fun finalize(outLen: Int = Blake3Ffm.OUT_LEN): ByteArray {
        val result = ByteArray(outLen)
        finalize(result, 0, outLen)
        return result
    }

    fun reset() {
        val segment = hasherSegment ?: error("Blake3StreamFfm is closed")
        Blake3Ffm.blake3_hasher_reset_handle.invokeExact(segment)
    }

    override fun close() {
        val currentArena = arena
        if (currentArena != null) {
            hasherSegment = null
            arena = null
            try {
                currentArena.close()
            } catch (_: Throwable) {}
        }
    }
}
