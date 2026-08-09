package com.akhsaul.blake3

public object Blake3 {
    public const val KEY_LEN: Int = 32
    public const val OUT_LEN: Int = 32
    public const val BLOCK_LEN: Int = 64
    public const val CHUNK_LEN: Int = 1024

    public fun hash(data: ByteArray, outLen: Int = OUT_LEN): ByteArray {
        val hasher = Blake3Hasher()
        return try {
            hasher.update(data)
            hasher.finalize(outLen)
        } finally {
            hasher.close()
        }
    }

    public fun keyedHash(key: ByteArray, data: ByteArray, outLen: Int = OUT_LEN): ByteArray {
        require(key.size == KEY_LEN) { "Key size must be $KEY_LEN bytes" }
        val hasher = Blake3Hasher(key)
        return try {
            hasher.update(data)
            hasher.finalize(outLen)
        } finally {
            hasher.close()
        }
    }
}
