package com.skiletro.wheelwitch.util.io

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

class FileDownloaderTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private lateinit var targetFile: File

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
        targetFile = File.createTempFile("filedownloader_test", ".bin")
        targetFile.delete()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
        targetFile.delete()
    }

    @Test
    fun `retries succeed after 2 transient failures`() {
        server.enqueue(MockResponse().setResponseCode(500).setBody("boom 1"))
        server.enqueue(MockResponse().setResponseCode(503).setBody("boom 2"))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "11")
                .setBody("hello world")
        )

        val result = FileDownloader.downloadToFile(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            client = client,
            maxRetries = 2,
            initialBackoffMillis = 10L,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.readText()).isEqualTo("hello world")
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `progress callback emits terminal 1f on eventual success`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "5")
                .setBody("12345")
        )

        val reports = mutableListOf<DownloadProgress>()
        FileDownloader.downloadToFile(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            onProgress = { reports.add(it) },
            client = client,
            maxRetries = 2,
            initialBackoffMillis = 10L,
        )

        assertThat(reports.last().progress).isEqualTo(1f)
    }

    @Test
    fun `gives up after 3 attempts when all fail with 500`() {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))

        val ex = runCatching {
            FileDownloader.downloadToFile(
                url = server.url("/file.bin").toString(),
                targetFile = targetFile,
                client = client,
                maxRetries = 2,
                initialBackoffMillis = 1L,
            )
        }.exceptionOrNull()

        assertThat(ex).isNotNull()
        assertThat(ex!!.message).contains("Download failed after 3 attempts")
        assertThat(server.requestCount).isEqualTo(3)
    }

    @Test
    fun `fails fast on HTTP 404 without retrying`() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("nope"))

        val ex = runCatching {
            FileDownloader.downloadToFile(
                url = server.url("/missing.bin").toString(),
                targetFile = targetFile,
                client = client,
                maxRetries = 2,
                initialBackoffMillis = 1L,
            )
        }.exceptionOrNull()

        assertThat(ex).isNotNull()
        assertThat(ex!!.message).contains("HTTP 404")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `fails fast on empty 200 body`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "0")
                .setBody("")
        )

        val ex = runCatching {
            FileDownloader.downloadToFile(
                url = server.url("/empty.bin").toString(),
                targetFile = targetFile,
                client = client,
                maxRetries = 2,
                initialBackoffMillis = 1L,
            )
        }.exceptionOrNull()

        assertThat(ex).isNotNull()
        assertThat(ex!!.message).contains("Empty response body")
        assertThat(server.requestCount).isEqualTo(1)
    }

    @Test
    fun `succeeds on first attempt with no retries needed`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "3")
                .setBody("abc")
        )

        val result = FileDownloader.downloadToFile(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            client = client,
            maxRetries = 2,
            initialBackoffMillis = 1L,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.readText()).isEqualTo("abc")
        assertThat(server.requestCount).isEqualTo(1)
    }
}
