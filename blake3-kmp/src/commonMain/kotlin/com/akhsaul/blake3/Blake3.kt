package com.akhsaul.blake3

expect object Blake3 {
    val KEY_LEN: Int
    val OUT_LEN: Int
    val BLOCK_LEN: Int
    val CHUNK_LEN: Int

    fun hash(
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray

    fun hashHex(
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String

    fun keyedHash(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
    ): ByteArray

    fun keyedHashHex(
        key: ByteArray,
        data: ByteArray,
        outLen: Int = OUT_LEN,
        format: HexFormat = HexFormat.Default,
    ): String
}
