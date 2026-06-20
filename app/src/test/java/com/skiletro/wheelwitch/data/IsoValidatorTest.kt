package com.skiletro.wheelwitch.data

import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.GameTypeParser.GameFormat
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class IsoValidatorTest {

  @Test
  fun `parseHeader returns Invalid for unknown game id`(@TempDir dir: File) {
    val file = File(dir, "unknown.iso").apply { writeBytes(ByteArray(64)) }
    val info = IsoValidator.parseHeader(file)
    assertThat(info).isNotNull()
    assertThat(info?.format).isEqualTo(GameFormat.Invalid)
  }

  @Test
  fun `parseHeader returns Iso with known game id`(@TempDir dir: File) {
    val buf = ByteArray(64)
    "RMCP01".encodeToByteArray().copyInto(buf, 0)
    val file = File(dir, "mkw.iso").apply { writeBytes(buf) }
    val info = IsoValidator.parseHeader(file)
    assertThat(info?.format).isEqualTo(GameFormat.Iso)
    assertThat(info?.gameId).isEqualTo("RMCP01")
  }

  @Test
  fun `parseHeader returns Rvz with known game id at offset 0x58`(@TempDir dir: File) {
    val buf = ByteArray(0x60)
    buf[0] = 0x52.toByte(); buf[1] = 0x56.toByte(); buf[2] = 0x5A.toByte(); buf[3] = 0x01.toByte()
    "RMCE01".encodeToByteArray().copyInto(buf, 0x58)
    val file = File(dir, "mkw.rvz").apply { writeBytes(buf) }
    val info = IsoValidator.parseHeader(file)
    assertThat(info?.format).isEqualTo(GameFormat.Rvz)
    assertThat(info?.gameId).isEqualTo("RMCE01")
  }

  @Test
  fun `parseHeader returns Invalid for truncated file`(@TempDir dir: File) {
    val file = File(dir, "tiny.iso").apply { writeBytes(ByteArray(0)) }
    val info = IsoValidator.parseHeader(file)
    assertThat(info?.format).isEqualTo(GameFormat.Invalid)
  }

  @Test
  fun `parseHeader returns null for missing file`(@TempDir dir: File) {
    val file = File(dir, "missing.iso")
    assertThat(IsoValidator.parseHeader(file)).isNull()
  }
}
