package com.akhsaul.blake3

internal expect fun loadNativeLibrary()

internal expect fun ByteArray.convertToHex(format: HexFormat = HexFormat.Default): String
