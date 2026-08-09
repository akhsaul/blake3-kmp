package com.akhsaul.blake3

internal external fun createHasher(): Long
internal external fun createKeyedHasher(key: ByteArray): Long
internal external fun hasherUpdate(hasherPtr: Long, input: ByteArray, offset: Int, length: Int)
internal external fun hasherFinalize(hasherPtr: Long, output: ByteArray, offset: Int, length: Int)
internal external fun hasherReset(hasherPtr: Long)
internal external fun freeHasher(hasherPtr: Long)
internal external fun hash(input: ByteArray, offset: Int, length: Int, output: ByteArray, outLen: Int)
internal external fun keyedHash(key: ByteArray, input: ByteArray, offset: Int, length: Int, output: ByteArray, outLen: Int)
