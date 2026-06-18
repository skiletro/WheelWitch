package com.skiletro.wheelwitch.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class ByteReaderTest {

    @Test
    fun `readASCII reads text at given offset`() {
        val bytes = byteArrayOf(0x41, 0x42, 0x43, 0x44, 0x45)
        assertThat(ByteReader.readASCII(bytes, 0, 3)).isEqualTo("ABC")
        assertThat(ByteReader.readASCII(bytes, 2, 3)).isEqualTo("CDE")
    }

    @Test
    fun `readASCII returns empty for short buffer`() {
        val bytes = byteArrayOf(0x41, 0x42)
        assertThat(ByteReader.readASCII(bytes, 0, 5)).isEqualTo("")
    }

    @Test
    fun `readUInt16BE reads correct values`() {
        val bytes = byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0x00)
        assertThat(ByteReader.readUInt16BE(bytes, 0)).isEqualTo(0x1234)
        assertThat(ByteReader.readUInt16BE(bytes, 2)).isEqualTo(0xFF00)
    }

    @Test
    fun `readUInt16BE returns 0 for short buffer`() {
        assertThat(ByteReader.readUInt16BE(byteArrayOf(0x12), 0)).isEqualTo(0)
    }

    @Test
    fun `readUInt32BE reads correct values`() {
        val bytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0xFF.toByte(), 0x00, 0x00, 0x00)
        assertThat(ByteReader.readUInt32BE(bytes, 0)).isEqualTo(0x01020304L)
        assertThat(ByteReader.readUInt32BE(bytes, 4)).isEqualTo(0xFF000000L)
    }

    @Test
    fun `readUInt32BE returns 0 for short buffer`() {
        assertThat(ByteReader.readUInt32BE(byteArrayOf(0x01, 0x02, 0x03), 0)).isEqualTo(0)
    }

    @Test
    fun `readInt32BE returns signed value`() {
        val bytes = byteArrayOf(0xFF.toByte(), 0x00, 0x00, 0x01)
        assertThat(ByteReader.readInt32BE(bytes, 0)).isEqualTo(-16777215)
    }

    @Test
    fun `readUTF16BE reads text`() {
        val bytes = byteArrayOf(0x00, 0x48, 0x00, 0x69, 0x00, 0x00)
        assertThat(ByteReader.readUTF16BE(bytes, 0, 6)).isEqualTo("Hi")
    }

    @Test
    fun `readUTF16BE stops at null terminator`() {
        val bytes = byteArrayOf(0x00, 0x41, 0x00, 0x00, 0x00, 0x42)
        assertThat(ByteReader.readUTF16BE(bytes, 0, 6)).isEqualTo("A")
    }

    @Test
    fun `readUTF16BE returns empty for short buffer`() {
        assertThat(ByteReader.readUTF16BE(byteArrayOf(0x00), 0, 4)).isEqualTo("")
    }

    @Test
    fun `checkMagic matches at given offset`() {
        val bytes = byteArrayOf(0x57, 0x42, 0x46, 0x53, 0x00)
        assertThat(ByteReader.checkMagic(bytes, 0, byteArrayOf(0x57, 0x42, 0x46, 0x53))).isTrue()
        assertThat(ByteReader.checkMagic(bytes, 0, byteArrayOf(0x57, 0x42, 0x46))).isTrue()
        assertThat(ByteReader.checkMagic(bytes, 0, byteArrayOf(0x00, 0x00, 0x00, 0x20))).isFalse()
    }

    @Test
    fun `checkMagic returns false for short buffer`() {
        assertThat(
            ByteReader.checkMagic(
                byteArrayOf(0x57, 0x42),
                0,
                byteArrayOf(0x57, 0x42, 0x46)
            )
        ).isFalse()
    }
}
