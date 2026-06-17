package com.skiletro.wheelwitch.data

import android.util.Base64
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import java.security.MessageDigest

object RksysParser {
    private val LICENSE_BASES = intArrayOf(0x08, 0x8CC8, 0x11988, 0x1A648)

    fun parse(bytes: ByteArray): SaveFileInfo {
        val licenses = LICENSE_BASES.mapIndexed { index, base ->
            parseLicense(bytes, base, index)
        }
        return SaveFileInfo(licenses)
    }

    private fun parseLicense(bytes: ByteArray, base: Int, slotIndex: Int): LicenseInfo {
        if (base + 4 > bytes.size) {
            return LicenseInfo(slotIndex = slotIndex, exists = false)
        }

        val magic = String(bytes, base, 4, Charsets.US_ASCII)
        if (magic != "RKPD") {
            return LicenseInfo(slotIndex = slotIndex, exists = false)
        }

        val miiName = readUTF16BE(bytes, base + 0x14, 20)
        val pid = readUInt32BE(bytes, base + 0x5C)
        val vr = readUInt16BE(bytes, base + 0xB0)
        val br = readUInt16BE(bytes, base + 0xB2)
        val raceWins = readInt32BE(bytes, base + 0x88)
        val raceLosses = readInt32BE(bytes, base + 0x8C)
        val battleWins = readInt32BE(bytes, base + 0x90)
        val battleLosses = readInt32BE(bytes, base + 0x94)

        val miiRflOffset = base + 0x5684
        val miiDataBase64 = if (miiRflOffset + 74 <= bytes.size) {
            val rflData = bytes.copyOfRange(miiRflOffset, miiRflOffset + 74)
            Base64.encodeToString(rflData, Base64.NO_WRAP)
        } else null

        return LicenseInfo(
            slotIndex = slotIndex,
            exists = true,
            miiName = miiName,
            friendCode = pidToFriendCode(pid),
            vr = vr,
            br = br,
            raceWins = raceWins,
            raceLosses = raceLosses,
            battleWins = battleWins,
            battleLosses = battleLosses,
            miiDataBase64 = miiDataBase64
        )
    }

    private fun pidToFriendCode(pid: Long): String {
        val buffer = ByteArray(8)
        buffer[0] = (pid and 0xFF).toByte()
        buffer[1] = ((pid shr 8) and 0xFF).toByte()
        buffer[2] = ((pid shr 16) and 0xFF).toByte()
        buffer[3] = ((pid shr 24) and 0xFF).toByte()
        buffer[4] = 'J'.code.toByte()
        buffer[5] = 'C'.code.toByte()
        buffer[6] = 'M'.code.toByte()
        buffer[7] = 'R'.code.toByte()

        val md5 = MessageDigest.getInstance("MD5").digest(buffer)
        val checksum = (md5[0].toInt() and 0xFF) shr 1
        val fc = (checksum.toLong() shl 32) or pid

        val fcStr = fc.toString().padStart(12, '0')
        return "${fcStr.substring(0, 4)}-${fcStr.substring(4, 8)}-${fcStr.substring(8, 12)}"
    }

    private fun readUInt16BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 2 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun readUInt32BE(bytes: ByteArray, offset: Int): Long {
        if (offset + 4 > bytes.size) return 0
        return (((bytes[offset].toInt() and 0xFF).toLong() shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF).toLong() shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF).toLong() shl 8) or
                (bytes[offset + 3].toInt() and 0xFF).toLong())
    }

    private fun readInt32BE(bytes: ByteArray, offset: Int): Int {
        if (offset + 4 > bytes.size) return 0
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
                ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
                ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
                (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun readUTF16BE(bytes: ByteArray, offset: Int, maxBytes: Int): String {
        val sb = StringBuilder()
        for (i in offset until offset + maxBytes step 2) {
            if (i + 2 > bytes.size) break
            val codePoint = ((bytes[i].toInt() and 0xFF) shl 8) or (bytes[i + 1].toInt() and 0xFF)
            if (codePoint == 0) break
            sb.append(codePoint.toChar())
        }
        return sb.toString()
    }
}
