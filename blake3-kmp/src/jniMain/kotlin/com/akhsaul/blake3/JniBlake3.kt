package com.akhsaul.blake3

object JniBlake3 {
    @JvmStatic
    @JvmName("createHasher")
    internal external fun createHasher(): Long

    @JvmStatic
    @JvmName("createKeyedHasher")
    internal external fun createKeyedHasher(key: ByteArray): Long

    @JvmStatic
    @JvmName("hasherUpdate")
    internal external fun hasherUpdate(
        hasherPtr: Long,
        input: ByteArray,
        offset: Int,
        length: Int,
    )

    @JvmStatic
    @JvmName("hasherFinalize")
    internal external fun hasherFinalize(
        hasherPtr: Long,
        output: ByteArray,
        offset: Int,
        length: Int,
    )

    @JvmStatic
    @JvmName("hasherReset")
    internal external fun hasherReset(hasherPtr: Long)

    @JvmStatic
    @JvmName("freeHasher")
    internal external fun freeHasher(hasherPtr: Long)

    @JvmStatic
    @JvmName("hash")
    internal external fun hash(
        input: ByteArray,
        offset: Int,
        length: Int,
        output: ByteArray,
        outLen: Int,
    )

    @JvmStatic
    @JvmName("keyedHash")
    internal external fun keyedHash(
        key: ByteArray,
        input: ByteArray,
        offset: Int,
        length: Int,
        output: ByteArray,
        outLen: Int,
    )
}
