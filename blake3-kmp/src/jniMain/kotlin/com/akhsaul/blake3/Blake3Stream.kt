package com.akhsaul.blake3

actual class Blake3Stream : AutoCloseable {
    private var hasherPtr: Long = 0

    init {
        loadNativeLibrary()
    }

    actual constructor() {
        hasherPtr = createHasher()
        check(hasherPtr != 0L) { "Failed to allocate native BLAKE3 hasher" }
    }

    actual constructor(key: ByteArray) {
        require(key.size == Blake3.KEY_LEN) { "Key size must be ${Blake3.KEY_LEN} bytes" }
        hasherPtr = createKeyedHasher(key)
        check(hasherPtr != 0L) { "Failed to allocate native BLAKE3 keyed hasher" }
    }

    actual fun update(
        input: ByteArray,
        offset: Int,
        length: Int,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= input.size) { "Index out of bounds" }
        check(hasherPtr != 0L) { "Blake3Stream has been closed" }
        hasherUpdate(hasherPtr, input, offset, length)
    }

    actual fun finalize(
        out: ByteArray,
        offset: Int,
        length: Int,
    ) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= out.size) { "Index out of bounds" }
        check(hasherPtr != 0L) { "Blake3Stream has been closed" }
        hasherFinalize(hasherPtr, out, offset, length)
    }

    actual fun finalize(outLen: Int): ByteArray {
        val result = ByteArray(outLen)
        finalize(result, 0, outLen)
        return result
    }

    actual fun reset() {
        check(hasherPtr != 0L) { "Blake3Stream has been closed" }
        hasherReset(hasherPtr)
    }

    actual override fun close() {
        if (hasherPtr != 0L) {
            freeHasher(hasherPtr)
            hasherPtr = 0L
        }
    }
}
