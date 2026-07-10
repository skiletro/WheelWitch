package com.skiletro.wheelwitch.data

object RRRatingParser {

  private const val MAGIC = "RRRT"
  private const val ENTRY_SIZE = 16

  data class RatingEntry(
    val profileId: Int,
    val vr: Float,
    val br: Float,
    val flags: Int,
  )

  fun parse(bytes: ByteArray): List<RatingEntry> {
    if (bytes.size < 8) return emptyList()
    val magic = String(bytes, 0, 4, Charsets.US_ASCII)
    if (magic != MAGIC) return emptyList()
    val version = ((bytes[4].toInt() and 0xFF) shl 8) or (bytes[5].toInt() and 0xFF)
    if (version != 1) return emptyList()
    val count = ((bytes[6].toInt() and 0xFF) shl 8) or (bytes[7].toInt() and 0xFF)
    val maxEntries = count.coerceAtMost(10000)
    val entries = mutableListOf<RatingEntry>()
    var off = 8
    for (i in 0 until maxEntries) {
      if (off + ENTRY_SIZE > bytes.size) break
      val profileId = ((bytes[off].toInt() and 0xFF) shl 24) or
        ((bytes[off + 1].toInt() and 0xFF) shl 16) or
        ((bytes[off + 2].toInt() and 0xFF) shl 8) or
        (bytes[off + 3].toInt() and 0xFF)
      val vrBits = ((bytes[off + 4].toInt() and 0xFF) shl 24) or
        ((bytes[off + 5].toInt() and 0xFF) shl 16) or
        ((bytes[off + 6].toInt() and 0xFF) shl 8) or
        (bytes[off + 7].toInt() and 0xFF)
      val brBits = ((bytes[off + 8].toInt() and 0xFF) shl 24) or
        ((bytes[off + 9].toInt() and 0xFF) shl 16) or
        ((bytes[off + 10].toInt() and 0xFF) shl 8) or
        (bytes[off + 11].toInt() and 0xFF)
      val flags = ((bytes[off + 12].toInt() and 0xFF) shl 24) or
        ((bytes[off + 13].toInt() and 0xFF) shl 16) or
        ((bytes[off + 14].toInt() and 0xFF) shl 8) or
        (bytes[off + 15].toInt() and 0xFF)
      val vr = Float.fromBits(vrBits)
      val br = Float.fromBits(brBits)
      if (profileId > 0 && vr.isFinite()) {
        entries.add(RatingEntry(profileId, vr, br, flags))
      }
      off += ENTRY_SIZE
    }
    return entries
  }
}
