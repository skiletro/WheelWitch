package com.skiletro.wheelwitch.util.io

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

    // --- Tier 1: Accept-Encoding identity on the single-stream path ---

    @Test
    fun `downloadToFile sends Accept-Encoding identity header`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "3")
                .setBody("abc")
        )

        FileDownloader.downloadToFile(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            client = client,
            maxRetries = 0,
        )

        val recorded = server.takeRequest()
        assertThat(recorded.getHeader("Accept-Encoding")).isEqualTo("identity")
    }

    // --- Tier 2: downloadInParallel ---

    /**
     * Installs a dispatcher that:
     * - Replies to HEAD with `Accept-Ranges: bytes` and a Content-Length
     * - Replies to GET with a `Range: bytes=start-end` header with the
     *   matching slice of [content] as a 206 Partial Content response.
     */
    private fun installRangeDispatcher(content: ByteArray) {
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.method == "HEAD") {
                        return MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Length", content.size.toString())
                            .setHeader("Accept-Ranges", "bytes")
                    }
                    val range = request.getHeader("Range")
                        ?: return MockResponse().setResponseCode(400)
                    val (start, end) = parseRange(range)
                    val slice = content.copyOfRange(start.toInt(), (end + 1).toInt())
                    return MockResponse()
                        .setResponseCode(206)
                        .setHeader(
                            "Content-Range",
                            "bytes $start-$end/${content.size}",
                        )
                        .setBody(Buffer().write(slice))
                }
            }
    }

    private fun parseRange(header: String): Pair<Long, Long> {
        // "bytes=start-end"
        val parts = header.removePrefix("bytes=").split("-")
        return parts[0].toLong() to parts[1].toLong()
    }

    @Test
    fun `downloadInParallel issues 4 Range requests when the server advertises bytes support`() {
        val content = ByteArray(1024) { (it % 256).toByte() }
        installRangeDispatcher(content)

        val result = FileDownloader.downloadInParallel(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            parallelism = 4,
            client = client,
            maxRetries = 0,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.length()).isEqualTo(content.size.toLong())
        assertThat(targetFile.readBytes()).isEqualTo(content)
        // 1 HEAD + 4 GET-with-Range
        assertThat(server.requestCount).isEqualTo(5)
    }

    @Test
    fun `downloadInParallel falls back to single-stream when server returns no Accept-Ranges`() {
        // Default dispatcher: enqueue a HEAD (no Accept-Ranges) then a 200
        // body. Only the 200 should be consumed by the GET — the parallel
        // path is skipped entirely.
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "5")
                .setHeader("Accept-Ranges", "none")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Length", "5")
                .setBody("hello")
        )

        val result = FileDownloader.downloadInParallel(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            parallelism = 4,
            client = client,
            maxRetries = 0,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.readText()).isEqualTo("hello")
        // HEAD probe + single-stream GET, no Range requests.
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `downloadInParallel falls back when Content-Length is missing`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Accept-Ranges", "bytes")
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("chunked-body")
        )

        val result = FileDownloader.downloadInParallel(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            parallelism = 4,
            client = client,
            maxRetries = 0,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.readText()).isEqualTo("chunked-body")
        assertThat(server.requestCount).isEqualTo(2)
    }

    @Test
    fun `downloadInParallel retries a chunk on transient 503 and then succeeds`() {
        val content = ByteArray(1024) { (it % 256).toByte() }
        val firstChunkAttempts = AtomicInteger(0)
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.method == "HEAD") {
                        return MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Length", content.size.toString())
                            .setHeader("Accept-Ranges", "bytes")
                    }
                    val range = request.getHeader("Range")
                        ?: return MockResponse().setResponseCode(400)
                    val (start, end) = parseRange(range)
                    // The first chunk (bytes 0-255) is the one that gets
                    // a 503 on its first attempt; the dispatcher hands
                    // out 503 / 200 / 200 / 200 in rotation. Track
                    // attempt count for the first chunk specifically.
                    if (start == 0L) {
                        val n = firstChunkAttempts.incrementAndGet()
                        if (n == 1) return MockResponse().setResponseCode(503)
                    }
                    val slice = content.copyOfRange(start.toInt(), (end + 1).toInt())
                    return MockResponse()
                        .setResponseCode(206)
                        .setHeader(
                            "Content-Range",
                            "bytes $start-$end/${content.size}",
                        )
                        .setBody(Buffer().write(slice))
                }
            }

        val result = FileDownloader.downloadInParallel(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            parallelism = 4,
            client = client,
            maxRetries = 2,
            initialBackoffMillis = 1L,
        )

        assertThat(result).isEqualTo(targetFile)
        assertThat(targetFile.readBytes()).isEqualTo(content)
        assertThat(firstChunkAttempts.get()).isAtLeast(2)
    }

    @Test
    fun `downloadInParallel deletes the partial file when a chunk exhausts retries`() {
        val content = ByteArray(1024) { (it % 256).toByte() }
        server.dispatcher =
            object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    if (request.method == "HEAD") {
                        return MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Length", content.size.toString())
                            .setHeader("Accept-Ranges", "bytes")
                    }
                    return MockResponse().setResponseCode(503)
                }
            }

        val ex = runCatching {
            FileDownloader.downloadInParallel(
                url = server.url("/file.bin").toString(),
                targetFile = targetFile,
                parallelism = 4,
                client = client,
                maxRetries = 1,
                initialBackoffMillis = 1L,
            )
        }.exceptionOrNull()

        assertThat(ex).isNotNull()
        // The pre-allocated file must be cleaned up on failure.
        assertThat(targetFile.exists()).isFalse()
    }

    @Test
    fun `downloadInParallel emits progress with monotonically increasing bytesDownloaded`() {
        val content = ByteArray(1024) { (it % 256).toByte() }
        installRangeDispatcher(content)

        val reports = mutableListOf<ParallelDownloadProgress>()
        FileDownloader.downloadInParallel(
            url = server.url("/file.bin").toString(),
            targetFile = targetFile,
            parallelism = 4,
            onProgress = { reports.add(it) },
            client = client,
            maxRetries = 0,
        )

        assertThat(reports).isNotEmpty()
        val downloaded = reports.map { it.bytesDownloaded }
        // Strictly monotonic (or equal between back-to-back ticks; we
        // assert that bytesDownloaded is non-decreasing and the final
        // report hits totalBytes).
        for (i in 1 until downloaded.size) {
            assertThat(downloaded[i]).isAtLeast(downloaded[i - 1])
        }
        assertThat(reports.last().bytesDownloaded).isEqualTo(content.size.toLong())
        assertThat(reports.last().progress).isEqualTo(1f)
    }
}
