package com.akhsaul.blake3

actual object Blake3 {
    actual const val KEY_LEN: Int = 32
    actual const val OUT_LEN: Int = 32
    actual const val BLOCK_LEN: Int = 64
    actual const val CHUNK_LEN: Int = 1024

    actual fun hash(
        data: ByteArray,
        outLen: Int,
    ): ByteArray {
        loadNativeLibrary()
        require(outLen > 0) { "Output length must be positive" }
        val result = ByteArray(outLen)
        hash(data, 0, data.size, result, outLen)
        return result
    }

    actual fun hashHex(
        data: ByteArray,
        outLen: Int,
        format: HexFormat,
    ): String = this.hash(data, outLen).convertToHex(format)

    actual fun keyedHash(
        key: ByteArray,
        data: ByteArray,
        outLen: Int,
    ): ByteArray {
        loadNativeLibrary()
        require(key.size == KEY_LEN) { "Key size must be $KEY_LEN bytes" }
        require(outLen > 0) { "Output length must be positive" }
        val result = ByteArray(outLen)
        keyedHash(key, data, 0, data.size, result, outLen)
        return result
    }

    actual fun keyedHashHex(
        key: ByteArray,
        data: ByteArray,
        outLen: Int,
        format: HexFormat,
    ): String = this.keyedHash(key, data, outLen).convertToHex(format)
}

internal expect fun ByteArray.convertToHex(format: HexFormat = HexFormat.Default): String
