package com.skiletro.wheelwitch.domain

import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.io.FileDownloader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RewindPackManagerTest {

  private lateinit var context: android.content.Context
  private lateinit var tree: DolphinTree
  private lateinit var cacheDir: File

  @BeforeEach
  fun setUp(@TempDir tempDir: File) {
    context = mockk(relaxed = true)
    tree = mockk(relaxed = true)
    cacheDir = tempDir
    every { context.cacheDir } returns cacheDir
    mockkObject(VersionFileParser)
    mockkObject(FileDownloader)
  }

  @AfterEach
  fun tearDown() {
    unmockkObject(VersionFileParser)
    unmockkObject(FileDownloader)
  }

  // --- checkStatus -----------------------------------------------------

  @Test
  fun `checkStatus returns NotInstalled when no local version and server reachable`() =
    runBlocking {
      coEvery { tree.readVersion() } returns null
      every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

      val status = manager().checkStatus()

      assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

  @Test
  fun `checkStatus returns UpdateAvailable when local is behind server`() = runBlocking {
    coEvery { tree.readVersion() } returns SemVersion(3, 2, 5)
    every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

    val status = manager().checkStatus()

    assertThat(status).isInstanceOf(PackStatus.UpdateAvailable::class.java)
    val ua = status as PackStatus.UpdateAvailable
    assertThat(ua.currentVersion).isEqualTo(SemVersion(3, 2, 5))
    assertThat(ua.latestVersion).isEqualTo(SemVersion(3, 2, 6))
  }

  @Test
  fun `checkStatus returns UpToDate when local matches server`() = runBlocking {
    coEvery { tree.readVersion() } returns SemVersion(3, 2, 6)
    every { VersionFileParser.fetchServerInfo() } returns Result.success(serverInfo())

    val status = manager().checkStatus()

    assertThat(status).isEqualTo(
      PackStatus.UpToDate(SemVersion(3, 2, 6), SemVersion(3, 2, 6))
    )
  }

  @Test
  fun `checkStatus returns Installed when server unreachable and local version exists`() =
    runBlocking {
      coEvery { tree.readVersion() } returns SemVersion(3, 2, 5)
      every { VersionFileParser.fetchServerInfo() } returns
        Result.failure(Exception("Network error"))

      val status = manager().checkStatus()

      assertThat(status).isEqualTo(PackStatus.Installed(SemVersion(3, 2, 5)))
    }

  @Test
  fun `checkStatus returns NotInstalled when server unreachable and no local version`() =
    runBlocking {
      coEvery { tree.readVersion() } returns null
      every { VersionFileParser.fetchServerInfo() } returns
        Result.failure(Exception("Network error"))

      val status = manager().checkStatus()

      assertThat(status).isEqualTo(PackStatus.NotInstalled)
    }

  @Test
  fun `checkStatus surfaces exceptions as they bubble up`() = runBlocking {
    // The view-model layer wraps checkStatus in runCatching; the
    // manager itself lets exceptions propagate so the caller decides
    // how to handle them.
    coEvery { tree.readVersion() } throws IllegalStateException("SAF boom")

    val ex = runCatching { manager().checkStatus() }.exceptionOrNull()
    assertThat(ex).isInstanceOf(IllegalStateException::class.java)
  }

  // --- installLatest ---------------------------------------------------

  @Test
  fun `installLatest downloads the full zip, extracts, and writes the version`() = runBlocking {
    val server = serverInfo()
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { VersionFileParser.getFullZipUrl() } returns "https://example.com/RetroRewind.zip"
    val progressReports = mutableListOf<RewindPackManager.InstallProgress>()
    // Simulate a download: just touch the target file.
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) // ZIP magic
        target
      }
    coEvery { tree.countZipFileEntries(any()) } returns 1
    coEvery { tree.extractZipToPack(any(), any()) } returns Unit
    coEvery { tree.writeVersion(server.latestVersion) } returns Unit

    val result =
      manager().installLatest { progress -> progressReports.add(progress) }

    assertThat(result.isSuccess).isTrue()
    verify(exactly = 1) {
      FileDownloader.downloadToFile(
        url = "https://example.com/RetroRewind.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
    coVerify(exactly = 1) { tree.extractZipToPack(any(), any()) }
    coVerify(exactly = 1) { tree.writeVersion(server.latestVersion) }
  }

  @Test
  fun `installLatest emits Downloading then Extracting phases in order`() = runBlocking {
    val server = serverInfo()
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { VersionFileParser.getFullZipUrl() } returns "https://example.com/RetroRewind.zip"
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        target
      }
    val phases = mutableListOf<RewindPackManager.InstallProgress>()
    coEvery { tree.countZipFileEntries(any()) } returns 3
    coEvery { tree.extractZipToPack(any(), any()) } coAnswers
      {
        val cb = it.invocation.args[1] as (Int) -> Unit
        cb(0)
        cb(1)
        cb(2)
      }
    coEvery { tree.writeVersion(server.latestVersion) } returns Unit

    val result = manager().installLatest { phase -> phases.add(phase) }

    assertThat(result.isSuccess).isTrue()
    // 1. First three reports are Downloading (one from the downloader's
    //    start-of-attempt hook, one at the end of the success path, plus
    //    a third the throttled callback adds — we accept any Downloading
    //    prefix and only assert the *eventual* transition to Extracting).
    // 2. The Extracting phase reports 4 entries (1 start + 3 done).
    assertThat(phases.filterIsInstance<RewindPackManager.InstallProgress.Extracting>())
      .containsExactly(
        RewindPackManager.InstallProgress.Extracting(filesDone = 0, filesTotal = 3),
        RewindPackManager.InstallProgress.Extracting(filesDone = 1, filesTotal = 3),
        RewindPackManager.InstallProgress.Extracting(filesDone = 2, filesTotal = 3),
        RewindPackManager.InstallProgress.Extracting(filesDone = 3, filesTotal = 3),
      )
      .inOrder()
    // The last phase is always Extracting, never Downloading.
    assertThat(phases.last())
      .isInstanceOf(RewindPackManager.InstallProgress.Extracting::class.java)
  }

  @Test
  fun `installLatest deletes the cached zip after the extract even on failure`() = runBlocking {
    val server = serverInfo()
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { VersionFileParser.getFullZipUrl() } returns "https://example.com/RetroRewind.zip"
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x00))
        target
      }
    coEvery { tree.countZipFileEntries(any()) } returns 1
    coEvery { tree.extractZipToPack(any(), any()) } throws IllegalStateException("extract boom")

    val result = manager().installLatest { /* no-op */ }
    assertThat(result.isFailure).isTrue()
    // No leftover zip in the cache dir.
    assertThat(cacheDir.listFiles()?.toList().orEmpty().any { it.name == "RetroRewind.zip" })
      .isFalse()
    // The version is NOT written because the extract failed.
    coVerify(exactly = 0) { tree.writeVersion(any()) }
  }

  @Test
  fun `installLatest returns failure when the server is unreachable`() = runBlocking {
    every { VersionFileParser.fetchServerInfo() } returns
      Result.failure(Exception("Server boom"))

    val result = manager().installLatest { /* no-op */ }
    assertThat(result.isFailure).isTrue()
    verify(exactly = 0) { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) }
  }

  // --- update ----------------------------------------------------------

  @Test
  fun `update falls back to full reinstall when no local version`() = runBlocking {
    coEvery { tree.readVersion() } returns null
    val server = serverInfo()
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { VersionFileParser.getFullZipUrl() } returns "https://example.com/full.zip"
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x00))
        target
      }
    coEvery { tree.countZipFileEntries(any()) } returns 1
    coEvery { tree.extractZipToPack(any(), any()) } returns Unit
    coEvery { tree.writeVersion(server.latestVersion) } returns Unit

    val result = manager().update { /* no-op */ }

    assertThat(result.isSuccess).isTrue()
    // Full zip URL used, not an incremental step URL.
    verify(exactly = 1) {
      FileDownloader.downloadToFile(
        url = "https://example.com/full.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
  }

  @Test
  fun `update falls back to full reinstall when local version is below 3_2_6`() = runBlocking {
    coEvery { tree.readVersion() } returns SemVersion(3, 2, 0)
    val server = serverInfo()
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { VersionFileParser.getFullZipUrl() } returns "https://example.com/full.zip"
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x00))
        target
      }
    coEvery { tree.countZipFileEntries(any()) } returns 1
    coEvery { tree.extractZipToPack(any(), any()) } returns Unit
    coEvery { tree.writeVersion(server.latestVersion) } returns Unit

    val result = manager().update { /* no-op */ }

    assertThat(result.isSuccess).isTrue()
    verify(exactly = 1) {
      FileDownloader.downloadToFile(
        url = "https://example.com/full.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
  }

  @Test
  fun `update applies incremental steps when local is at or above 3_2_6`() = runBlocking {
    coEvery { tree.readVersion() } returns SemVersion(3, 3, 0)
    val server =
      ServerInfo(
        latestVersion = SemVersion(3, 3, 2),
        allUpdates =
          listOf(
            UpdateEntry(
              SemVersion(3, 3, 0),
              "https://example.com/3.3.0.zip",
              "/x",
              "3.3.0",
            ),
            UpdateEntry(
              SemVersion(3, 3, 1),
              "https://example.com/3.3.1.zip",
              "/x",
              "3.3.1",
            ),
            UpdateEntry(
              SemVersion(3, 3, 2),
              "https://example.com/3.3.2.zip",
              "/x",
              "3.3.2",
            ),
          ),
        deletions = emptyList(),
      )
    every { VersionFileParser.fetchServerInfo() } returns Result.success(server)
    every { FileDownloader.downloadToFile(any(), any(), any(), any(), any(), any()) } answers
      {
        val target = it.invocation.args[1] as File
        target.parentFile?.mkdirs()
        target.writeBytes(byteArrayOf(0x00))
        target
      }
    coEvery { tree.countZipFileEntries(any()) } returns 1
    coEvery { tree.extractZipToPack(any(), any()) } returns Unit
    coEvery { tree.writeVersion(server.latestVersion) } returns Unit

    val result = manager().update { /* no-op */ }

    assertThat(result.isSuccess).isTrue()
    // Only 3.3.1 and 3.3.2 are downloaded — 3.3.0 is the current local.
    verify(exactly = 1) {
      FileDownloader.downloadToFile(
        url = "https://example.com/3.3.1.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
    verify(exactly = 1) {
      FileDownloader.downloadToFile(
        url = "https://example.com/3.3.2.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
    // The full zip URL was NOT used.
    verify(exactly = 0) {
      FileDownloader.downloadToFile(
        url = "https://example.com/RetroRewind.zip",
        targetFile = any(),
        onProgress = any(),
        client = any(),
        maxRetries = any(),
        initialBackoffMillis = any(),
      )
    }
  }

  // --- helpers ---------------------------------------------------------

  private fun manager() = RewindPackManager(context, tree)

  private fun serverInfo(): ServerInfo =
    ServerInfo(
      latestVersion = SemVersion(3, 2, 6),
      allUpdates =
        listOf(
          UpdateEntry(
            SemVersion(3, 2, 6),
            "https://example.com/3.2.6.zip",
            "/x",
            "3.2.6",
          )
        ),
      deletions = emptyList(),
    )
}
