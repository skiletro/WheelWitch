package com.skiletro.wheelwitch.util.mii

import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class MiiFaceCacheTest {

    @Test
    fun `cacheSize returns 0 when dir is empty`(@TempDir tempDir: Path) {
        val dir = File(tempDir.toFile(), "mii_faces").also { it.mkdirs() }
        assertThat(MiiFaceCache.cacheSize(dir)).isEqualTo(0L)
    }

    @Test
    fun `cacheSize returns sum of file sizes`(@TempDir tempDir: Path) {
        val dir = File(tempDir.toFile(), "mii_faces").also { it.mkdirs() }
        File(dir, "a.png").writeBytes(ByteArray(100))
        File(dir, "b.png").writeBytes(ByteArray(250))
        assertThat(MiiFaceCache.cacheSize(dir)).isEqualTo(350L)
    }

    @Test
    fun `cacheSize returns 0 when dir does not exist`(@TempDir tempDir: Path) {
        val dir = File(tempDir.toFile(), "missing")
        assertThat(MiiFaceCache.cacheSize(dir)).isEqualTo(0L)
    }

    @Test
    fun `clear removes all files`(@TempDir tempDir: Path) {
        val dir = File(tempDir.toFile(), "mii_faces").also { it.mkdirs() }
        File(dir, "a.png").writeBytes(ByteArray(10))
        File(dir, "b.png").writeBytes(ByteArray(20))
        MiiFaceCache.clear(dir)
        assertThat(dir.listFiles()?.size ?: 0).isEqualTo(0)
    }

    @Test
    fun `init creates cache dir under provided target`(@TempDir tempDir: Path) {
        val expected = File(tempDir.toFile(), "mii_faces")
        MiiFaceCache.initWith(expected)
        assertThat(expected.exists()).isTrue()
    }
}
