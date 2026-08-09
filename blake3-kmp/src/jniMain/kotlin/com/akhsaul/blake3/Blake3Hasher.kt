package com.akhsaul.blake3

public actual class Blake3Hasher : AutoCloseable {
    private var hasherPtr: Long = 0

    init {
        loadNativeLibrary()
    }

    public actual constructor() {
        hasherPtr = createHasher()
        check(hasherPtr != 0L) { "Failed to allocate native BLAKE3 hasher" }
    }

    public actual constructor(key: ByteArray) {
        require(key.size == Blake3.KEY_LEN) { "Key size must be ${Blake3.KEY_LEN} bytes" }
        hasherPtr = createKeyedHasher(key)
        check(hasherPtr != 0L) { "Failed to allocate native BLAKE3 keyed hasher" }
    }

    public actual fun update(input: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= input.size) { "Index out of bounds" }
        check(hasherPtr != 0L) { "Blake3Hasher has been closed" }
        hasherUpdate(hasherPtr, input, offset, length)
    }

    public actual fun finalize(out: ByteArray, offset: Int, length: Int) {
        if (length <= 0) return
        require(offset >= 0 && offset + length <= out.size) { "Index out of bounds" }
        check(hasherPtr != 0L) { "Blake3Hasher has been closed" }
        hasherFinalize(hasherPtr, out, offset, length)
    }

    public actual fun finalize(outLen: Int): ByteArray {
        val result = ByteArray(outLen)
        finalize(result, 0, outLen)
        return result
    }

    public actual fun reset() {
        check(hasherPtr != 0L) { "Blake3Hasher has been closed" }
        hasherReset(hasherPtr)
    }

    public actual override fun close() {
        if (hasherPtr != 0L) {
            freeHasher(hasherPtr)
            hasherPtr = 0L
        }
    }
}
