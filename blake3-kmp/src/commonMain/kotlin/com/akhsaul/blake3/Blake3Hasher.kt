package com.akhsaul.blake3

public expect class Blake3Hasher : AutoCloseable {
    public constructor()
    public constructor(key: ByteArray)

    public fun update(input: ByteArray, offset: Int = 0, length: Int = input.size - offset)
    public fun finalize(out: ByteArray, offset: Int = 0, length: Int = out.size - offset)
    public fun finalize(outLen: Int = Blake3.OUT_LEN): ByteArray
    public fun reset()
    public override fun close()
}
