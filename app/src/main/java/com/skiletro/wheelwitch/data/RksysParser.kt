package com.skiletro.wheelwitch.data

import java.util.Base64
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.util.ByteReader
import java.security.MessageDigest

/** Parses a Mario Kart Wii `rksys.dat` save file (big-endian binary format, RKPD magic) into [SaveFileInfo]. */
object RksysParser {
    /**
     * Start offset of each of the 4 license slots inside `rksys.dat`.
     * Slot stride is `0x8CC0` bytes (`0x8CC8 - 0x08`).
     */
    private val LICENSE_BASES = intArrayOf(0x08, 0x8CC8, 0x11988, 0x1A648)

    /** All offsets below are relative to the start of a license slot ([LICENSE_BASES] entry). */
    private const val MII_NAME_OFFSET = 0x14
    /** 20 bytes = 10 UTF-16BE code units (2 bytes each). */
    private const val MII_NAME_LENGTH = 20
    private const val PID_OFFSET = 0x5C
    private const val RACE_WINS_OFFSET = 0x88
    private const val RACE_LOSSES_OFFSET = 0x8C
    private const val VR_OFFSET = 0xB0
    /** Offset of the RFL (Mii binary) payload. RFL is the raw 74-byte Mii data the Wii uses to render the face. */
    private const val MII_RFL_OFFSET = 0x5684
    /** Fixed length of the RFL Mii data payload. */
    private const val MII_RFL_DATA_LENGTH = 74

    /** Parses a raw `rksys.dat` byte array into [SaveFileInfo] with up to 4 license slots. */
    fun parse(bytes: ByteArray): SaveFileInfo {
        val licenses = LICENSE_BASES.mapIndexed { index, base ->
            parseLicense(bytes, base, index)
        }
        return SaveFileInfo(licenses)
    }

    /**
     * Reads one license slot at [base]. Returns `exists=false` when the
     * slot is absent (offset out of bounds) or lacks the `RKPD` magic.
     */
    private fun parseLicense(bytes: ByteArray, base: Int, slotIndex: Int): LicenseInfo {
        if (base + 4 > bytes.size) {
            return LicenseInfo(slotIndex = slotIndex, exists = false)
        }

        val magic = String(bytes, base, 4, Charsets.US_ASCII)
        if (magic != "RKPD") {
            return LicenseInfo(slotIndex = slotIndex, exists = false)
        }

        val miiName = ByteReader.readUTF16BE(bytes, base + MII_NAME_OFFSET, MII_NAME_LENGTH)
        val pid = ByteReader.readUInt32BE(bytes, base + PID_OFFSET)
        val vr = ByteReader.readUInt16BE(bytes, base + VR_OFFSET)
        val raceWins = ByteReader.readInt32BE(bytes, base + RACE_WINS_OFFSET)
        val raceLosses = ByteReader.readInt32BE(bytes, base + RACE_LOSSES_OFFSET)

        val miiDataBase64 = if (base + MII_RFL_OFFSET + MII_RFL_DATA_LENGTH <= bytes.size) {
            val rflData = bytes.copyOfRange(base + MII_RFL_OFFSET, base + MII_RFL_OFFSET + MII_RFL_DATA_LENGTH)
            Base64.getEncoder().encodeToString(rflData)
        } else null

        return LicenseInfo(
            slotIndex = slotIndex,
            exists = true,
            miiName = miiName,
            friendCode = pidToFriendCode(pid),
            vr = vr,
            raceWins = raceWins,
            raceLosses = raceLosses,
            miiDataBase64 = miiDataBase64
        )
    }

    /**
     * Converts a Wii PID into a 12-digit friend code (`XXXX-XXXX-XXXX`)
     * using the JCMR + MD5 checksum algorithm.
     *
     * The friend code is a 64-bit value whose layout is
     * `[checksum:8][pid:32]` (high 8 bits are a 7-bit checksum, low 32
     * bits are the PID), rendered as a 12-digit decimal.
     *
     * Buffer layout fed to MD5: PID (little-endian, 4 bytes) followed by
     * the ASCII string "JCMR" (the Wii region/product code constant).
     *
     * The checksum is `(md5[0] & 0xFF) >> 1` because the friend-code high
     * byte uses only 7 bits (the top bit is reserved).
     */
    private fun pidToFriendCode(pid: Long): String {
        // Layout: PID (LE, 4 bytes) || "JCMR" (ASCII, 4 bytes)
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
        // Top 7 bits of md5[0] form the friend-code high byte.
        val checksum = (md5[0].toInt() and 0xFF) shr 1
        // Pack [checksum:8][pid:32] into a 64-bit value.
        val fc = (checksum.toLong() shl 32) or pid

        val fcStr = fc.toString().padStart(12, '0')
        return "${fcStr.substring(0, 4)}-${fcStr.substring(4, 8)}-${fcStr.substring(8, 12)}"
    }


}
