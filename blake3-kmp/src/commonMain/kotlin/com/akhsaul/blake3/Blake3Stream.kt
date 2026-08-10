package com.akhsaul.blake3

expect class Blake3Stream : AutoCloseable {
    constructor()
    constructor(key: ByteArray)

    fun update(
        input: ByteArray,
        offset: Int = 0,
        length: Int = input.size - offset,
    )

    fun finalize(
        out: ByteArray,
        offset: Int = 0,
        length: Int = out.size - offset,
    )

    fun finalize(outLen: Int = Blake3.OUT_LEN): ByteArray

    fun reset()

    override fun close()
}
