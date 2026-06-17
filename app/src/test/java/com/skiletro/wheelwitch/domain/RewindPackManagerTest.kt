package com.skiletro.wheelwitch.domain

import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.FileDownloader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class RewindPackManagerTest {

    private val storage = mockk<PackStorage>(relaxed = true)

    @BeforeEach
    fun setUp() {
        mockkObject(VersionFileParser)
        mockkObject(FileDownloader)
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(VersionFileParser)
        unmockkObject(FileDownloader)
    }

    // --- checkStatus ---

    @Test
    fun `checkStatus returns NotInstalled when no local version and server reachable`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns null
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

    @Test
    fun `checkStatus returns UpdateAvailable when local is behind server`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns "3.2.5"
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isInstanceOf(PackStatus.UpdateAvailable::class.java)
        val ua = status as PackStatus.UpdateAvailable
        assertThat(ua.currentVersion).isEqualTo(SemVersion(3, 2, 5))
        assertThat(ua.latestVersion).isEqualTo(SemVersion(3, 2, 6))
    }

    @Test
    fun `checkStatus returns UpToDate when local matches server`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns "3.2.6"
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isEqualTo(
            PackStatus.UpToDate(SemVersion(3, 2, 6), SemVersion(3, 2, 6))
        )
    }

    @Test
    fun `checkStatus returns Installed when server unreachable and local version exists`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns "3.2.5"
        every { VersionFileParser.fetchServerInfo() } returns Result.failure(Exception("Network error"))

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isEqualTo(PackStatus.Installed(SemVersion(3, 2, 5)))
    }

    @Test
    fun `checkStatus returns NotInstalled when server unreachable and no local version`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns null
        every { VersionFileParser.fetchServerInfo() } returns Result.failure(Exception("Network error"))

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

    @Test
    fun `checkStatus returns NotInstalled when local version file is empty`() = runBlocking {
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns ""
        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

        val status = RewindPackManager.checkStatus(storage)

        assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

    // --- freshInstall ---

    @Test
    fun `freshInstall downloads zip and extracts it`() = runBlocking {
        val cacheDir = createTempDir()
        RewindPackManager.initCacheDir(cacheDir)

        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())
        every { VersionFileParser.getFullZipUrl() } returns "https://example.com/full.zip"
        every { FileDownloader.downloadToFile(any(), any(), any()) } answers {
            secondArg<File>().writeText("fake zip content")
            secondArg<File>()
        }
        every { storage.readFile(RewindPackManager.VERSION_FILE) } returns null
        every { storage.extractZip(any(), any()) } returns Result.success(Unit)

        val progress = mutableListOf<ProgressInfo>()
        val result = RewindPackManager.freshInstall(storage) { progress.add(it) }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(SemVersion(3, 2, 6))
        verify { storage.extractZip(any(), any()) }
        verify { storage.writeFile(RewindPackManager.VERSION_FILE, "3.2.6") }

        cacheDir.deleteRecursively()
    }

    @Test
    fun `freshInstall emits progress callbacks`() = runBlocking {
        val cacheDir = createTempDir()
        RewindPackManager.initCacheDir(cacheDir)

        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())
        every { VersionFileParser.getFullZipUrl() } returns "https://example.com/full.zip"
        every { FileDownloader.downloadToFile(any(), any(), any()) } answers {
            secondArg<File>().writeText("fake")
            secondArg<File>()
        }
        every { storage.extractZip(any(), any()) } returns Result.success(Unit)

        val progress = mutableListOf<ProgressInfo>()
        RewindPackManager.freshInstall(storage) { progress.add(it) }

        assertThat(progress).isNotEmpty()
        assertThat(progress.any { it is ProgressInfo.Downloading }).isTrue()
        assertThat(progress.any { it is ProgressInfo.Extracting }).isTrue()

        cacheDir.deleteRecursively()
    }

    // --- incrementalUpdate ---

    @Test
    fun `incrementalUpdate falls back to freshInstall when version below 3_2_6`() = runBlocking {
        val cacheDir = createTempDir()
        RewindPackManager.initCacheDir(cacheDir)

        every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())
        every { VersionFileParser.getFullZipUrl() } returns "https://example.com/full.zip"
        every { FileDownloader.downloadToFile(any(), any(), any()) } answers {
            secondArg<File>().writeText("fake")
            secondArg<File>()
        }
        every { storage.extractZip(any(), any()) } returns Result.success(Unit)

        val progress = mutableListOf<ProgressInfo>()
        val result = RewindPackManager.incrementalUpdate(
            storage, serverInfo(), SemVersion(3, 2, 0)
        ) { progress.add(it) }

        assertThat(result.isSuccess).isTrue()
        verify { storage.extractZip(any(), any()) }

        cacheDir.deleteRecursively()
    }

    @Test
    fun `incrementalUpdate downloads and extracts update steps in order`() = runBlocking {
        val cacheDir = createTempDir()
        RewindPackManager.initCacheDir(cacheDir)

        val steps = listOf(
            UpdateEntry(SemVersion(3, 2, 6), "https://a.zip", "/path", "Fix A"),
            UpdateEntry(SemVersion(3, 2, 7), "https://b.zip", "/path", "Fix B"),
        )
        val info = ServerInfo(SemVersion(3, 2, 7), steps, emptyList())

        every { FileDownloader.downloadToFile(any(), any(), any()) } answers {
            secondArg<File>().writeText("fake")
            secondArg<File>()
        }
        every { storage.extractZip(any(), any()) } returns Result.success(Unit)

        val progress = mutableListOf<ProgressInfo>()
        val result = RewindPackManager.incrementalUpdate(
            storage, info, SemVersion(3, 2, 5)
        ) { progress.add(it) }

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(SemVersion(3, 2, 7))
        verify(exactly = 2) { FileDownloader.downloadToFile(any(), any(), any()) }
        verify(exactly = 2) { storage.extractZip(any(), any()) }

        cacheDir.deleteRecursively()
    }

    @Test
    fun `incrementalUpdate applies deletions before downloading`() = runBlocking {
        val cacheDir = createTempDir()
        RewindPackManager.initCacheDir(cacheDir)

        val steps = listOf(
            UpdateEntry(SemVersion(3, 2, 6), "https://a.zip", "/path", "Fix A"),
        )
        val deletions = listOf(
            com.skiletro.wheelwitch.model.DeletionEntry(SemVersion(3, 2, 6), "/old/file.txt"),
        )
        val info = ServerInfo(SemVersion(3, 2, 6), steps, deletions)

        every { FileDownloader.downloadToFile(any(), any(), any()) } answers {
            secondArg<File>().writeText("fake")
            secondArg<File>()
        }
        every { storage.extractZip(any(), any()) } returns Result.success(Unit)

        val result = RewindPackManager.incrementalUpdate(
            storage, info, SemVersion(3, 2, 5)
        ) {}

        assertThat(result.isSuccess).isTrue()
        verify { storage.deleteFile("/old/file.txt") }

        cacheDir.deleteRecursively()
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
