package com.akhsaul.blake3.ffm

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

object Blake3Ffm {
    const val KEY_LEN: Int = 32
    const val OUT_LEN: Int = 32
    const val BLOCK_LEN: Int = 64
    const val CHUNK_LEN: Int = 1024

    private val linker: Linker by lazy { Linker.nativeLinker() }
    private val lookup: SymbolLookup by lazy {
        loadFfmNativeLibrary()
        SymbolLookup.loaderLookup()
    }

    private fun findSymbol(name: String): MemorySegment {
        return lookup.find(name).orElseGet {
            throw UnsatisfiedLinkError("Failed to find native symbol: $name")
        }
    }

    private val blake3_hasher_sizeof_handle: MethodHandle? by lazy {
        lookup.find("blake3_hasher_sizeof").map {
            linker.downcallHandle(it, FunctionDescriptor.of(ValueLayout.JAVA_LONG))
        }.orElse(null)
    }

    internal val blake3_hasher_init_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_init"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        )
    }

    internal val blake3_hasher_init_keyed_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_init_keyed"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        )
    }

    internal val blake3_hasher_update_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_update"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        )
    }

    internal val blake3_hasher_finalize_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_finalize"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
        )
    }

    internal val blake3_hasher_reset_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_reset"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        )
    }

    internal val hasherSize: Long by lazy {
        try {
            val handle = blake3_hasher_sizeof_handle
            if (handle != null) {
                (handle.invokeExact() as Number).toLong()
            } else {
                1912L
            }
        } catch (_: Throwable) {
            1912L
        }
    }

    fun hash(
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray {
        require(outLen > 0) { "Output length must be positive" }
        val result = ByteArray(outLen)
        val arena = Arena.ofConfined()
        try {
            val hasherSegment = arena.allocate(hasherSize)
            blake3_hasher_init_handle.invokeExact(hasherSegment)

            if (data.isNotEmpty()) {
                val inputSegment = arena.allocate(data.size.toLong())
                MemorySegment.copy(data, 0, inputSegment, ValueLayout.JAVA_BYTE, 0L, data.size)
                blake3_hasher_update_handle.invokeExact(hasherSegment, inputSegment, data.size.toLong())
            }

            val outputSegment = arena.allocate(outLen.toLong())
            blake3_hasher_finalize_handle.invokeExact(hasherSegment, outputSegment, outLen.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, result, 0, outLen)
        } finally {
            arena.close()
        }
        return result
    }

    fun hashHex(
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String = hash(data, outLen).toHexString(format)

    fun keyedHash(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray {
        require(key.size == KEY_LEN) { "Key size must be $KEY_LEN bytes" }
        require(outLen > 0) { "Output length must be positive" }
        val result = ByteArray(outLen)
        val arena = Arena.ofConfined()
        try {
            val hasherSegment = arena.allocate(hasherSize)
            val keySegment = arena.allocate(32L)
            MemorySegment.copy(key, 0, keySegment, ValueLayout.JAVA_BYTE, 0L, 32)
            blake3_hasher_init_keyed_handle.invokeExact(hasherSegment, keySegment)

            if (data.isNotEmpty()) {
                val inputSegment = arena.allocate(data.size.toLong())
                MemorySegment.copy(data, 0, inputSegment, ValueLayout.JAVA_BYTE, 0L, data.size)
                blake3_hasher_update_handle.invokeExact(hasherSegment, inputSegment, data.size.toLong())
            }

            val outputSegment = arena.allocate(outLen.toLong())
            blake3_hasher_finalize_handle.invokeExact(hasherSegment, outputSegment, outLen.toLong())
            MemorySegment.copy(outputSegment, ValueLayout.JAVA_BYTE, 0L, result, 0, outLen)
        } finally {
            arena.close()
        }
        return result
    }

    fun keyedHashHex(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String = keyedHash(key, data, outLen).toHexString(format)

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.toHexString(format: HexFormat): String {
        return this.toHexString(format)
    }
}

