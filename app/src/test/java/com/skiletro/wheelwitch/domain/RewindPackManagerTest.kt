package com.skiletro.wheelwitch.domain

import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.StoragePaths
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.network.VersionFileParser
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class RewindPackManagerTest {

    private lateinit var tempRoot: File

    @BeforeEach
    fun setUp() {
        tempRoot = createTempDir()
        mockkObject(VersionFileParser)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(VersionFileParser)
        tempRoot.deleteRecursively()
    }

    @Test
    fun `checkStatus returns NotInstalled when no local version and server reachable`() =
        runBlocking {
            every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

            val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

            assertThat(status).isEqualTo(PackStatus.NotInstalled)
        }

    @Test
    fun `checkStatus returns UpdateAvailable when local is behind server`() = runBlocking {
        writeLocalVersion("3.2.5")
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

        assertThat(status).isInstanceOf(PackStatus.UpdateAvailable::class.java)
        val ua = status as PackStatus.UpdateAvailable
        assertThat(ua.currentVersion).isEqualTo(SemVersion(3, 2, 5))
        assertThat(ua.latestVersion).isEqualTo(SemVersion(3, 2, 6))
    }

    @Test
    fun `checkStatus returns UpToDate when local matches server`() = runBlocking {
        writeLocalVersion("3.2.6")
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

        assertThat(status).isEqualTo(
            PackStatus.UpToDate(SemVersion(3, 2, 6), SemVersion(3, 2, 6))
        )
    }

    @Test
    fun `checkStatus returns Installed when server unreachable and local version exists`() =
        runBlocking {
            writeLocalVersion("3.2.5")
            every { VersionFileParser.fetchServerInfo() } returns
                Result.failure(Exception("Network error"))

            val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

            assertThat(status).isEqualTo(PackStatus.Installed(SemVersion(3, 2, 5)))
        }

    @Test
    fun `checkStatus returns NotInstalled when server unreachable and no local version`() =
        runBlocking {
            every { VersionFileParser.fetchServerInfo() } returns
                Result.failure(Exception("Network error"))

            val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

            assertThat(status).isEqualTo(PackStatus.NotInstalled)
        }

    @Test
    fun `checkStatus returns NotInstalled when local version file is empty`() = runBlocking {
        writeLocalVersion("")
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

        assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

    @Test
    fun `checkStatus returns NotInstalled when local version file is missing the RetroRewind6 folder`() =
        runBlocking {
            // No file written at all; root exists but version file does not.
            every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

            val status = RewindPackManager.checkStatus(storageRoot = tempRoot.absolutePath)

            assertThat(status).isEqualTo(PackStatus.NotInstalled)
        }

    @Test
    fun `StoragePaths versionFile uses the DEFAULT_ROOT by default`() {
        val file = StoragePaths.versionFile()
        assertThat(file.absolutePath).isEqualTo("/storage/emulated/0/RetroRewind6/version.txt")
    }

    @Test
    fun `StoragePaths versionFile honors a custom root`() {
        val file = StoragePaths.versionFile("/custom/root")
        assertThat(file.absolutePath).isEqualTo("/custom/root/RetroRewind6/version.txt")
    }

    private fun writeLocalVersion(version: String) {
        val file = StoragePaths.versionFile(tempRoot.absolutePath)
        file.parentFile?.mkdirs()
        file.writeText(version)
    }

    private fun serverInfo(): ServerInfo {
        val updates = listOf(
            UpdateEntry(SemVersion(3, 2, 6), "https://example.com/3.2.6.zip", "/path", "Update")
        )
        return ServerInfo(SemVersion(3, 2, 6), updates, emptyList())
    }

    private fun createTempDir(): File {
        val dir = File.createTempFile("rewind_test", "")
        dir.delete()
        dir.mkdirs()
        return dir
    }
}
