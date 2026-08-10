package com.akhsaul.blake3

internal actual fun ByteArray.convertToHex(format: HexFormat): String = this.toHexString(format)
