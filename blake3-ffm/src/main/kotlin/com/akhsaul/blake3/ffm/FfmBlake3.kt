package com.akhsaul.blake3.ffm

import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

object FfmBlake3 {
    private val linker: Linker by lazy { Linker.nativeLinker() }
    private val lookup: SymbolLookup by lazy {
        loadNativeLibrary()
        SymbolLookup.loaderLookup()
    }

    private fun findSymbol(name: String): MemorySegment =
        lookup.find(name).orElseGet {
            throw UnsatisfiedLinkError("Failed to find native symbol: $name")
        }

    private val blake3_hasher_sizeof_handle: MethodHandle? by lazy {
        lookup
            .find("blake3_hasher_sizeof")
            .map {
                linker.downcallHandle(it, FunctionDescriptor.of(ValueLayout.JAVA_LONG))
            }.orElse(null)
    }

    internal val blake3_hasher_init_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_init"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
        )
    }

    internal val blake3_hasher_init_keyed_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_init_keyed"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS),
        )
    }

    internal val blake3_hasher_update_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_update"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
        )
    }

    internal val blake3_hasher_finalize_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_finalize"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG),
        )
    }

    internal val blake3_hasher_reset_handle: MethodHandle by lazy {
        linker.downcallHandle(
            findSymbol("blake3_hasher_reset"),
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
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
}
