package com.skiletro.wheelwitch.util

object ByteReader {

    fun readASCII(bytes: ByteArray, offset: Int, length: Int): String {
        if (offset + length > bytes.size) return ""
        return bytes.copyOfRange(offset, offset + length).decodeToString()
    }

    fun readUInt16BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    fun readUInt32BE(bytes: ByteArray, offset: Int): Long {
        if (offset + 4 > bytes.size) return 0
        return (((bytes[offset].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (bytes[offset + 3].toInt() and 0xFF).toLong())
    }

    fun readInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    fun readUTF16BE(bytes: ByteArray, offset: Int, maxBytes: Int): String {
        if (offset + 2 > bytes.size) return ""
        val end = offset + maxBytes.coerceAtMost(bytes.size - offset)
        val sb = StringBuilder()
        for (i in offset until end step 2) {
            val codeUnit = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            if (codeUnit == 0) break
            sb.append(codeUnit.toChar())
        }
        return sb.toString()
    }

    fun checkMagic(bytes: ByteArray, offset: Int, expected: ByteArray): Boolean {
        if (offset + expected.size > bytes.size) return false
        for (i in expected.indices) {
            if (bytes[offset + i] != expected[i]) return false
        }
        return true
    }
}
