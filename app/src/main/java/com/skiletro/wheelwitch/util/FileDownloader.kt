package com.skiletro.wheelwitch.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Progress snapshot for an in-flight download, emitted to UI consumers. */
data class DownloadProgress(
    val progress: Float,
    val bytesPerSecond: Long,
    val bytesDownloaded: Long,
    val totalBytes: Long,
)

/** Shared HTTP download utility used by both [RewindPackManager] and [MiiWadInstaller]. */
object FileDownloader {
    private const val DOWNLOAD_BUFFER_SIZE = 256 * 1024

    /**
     * Downloads [url] to [targetFile]. Blocking — call from a background dispatcher.
     *
     * On transient failures (network [IOException] or HTTP 5xx), retries up to [maxRetries] times
     * with exponential backoff ([initialBackoffMillis] * 2^attempt). HTTP 4xx and other non-2xx
     * responses are treated as deterministic failures and are not retried. Throws on exhausted
     * retries, on a 4xx, or on a missing/empty body.
     *
     * @param onProgress Optional callback with progress and instantaneous byte rate. Throttled to
     *   ≥1% progress changes; always emits progress=1f at the end of a successful download. Reset
     *   to bytesPerSecond=0 at the start of each retry attempt so the UI does not show a stale rate
     *   during backoff.
     * @param client OkHttp client to use. Defaults to [HttpClientProvider.client];
     *   pass [HttpClientProvider.largeDownloadClient] for big payloads to
     *   avoid spurious 15s read timeouts.
     * @param maxRetries Number of retries after the first attempt (so 2 = up to 3 total attempts).
     * @param initialBackoffMillis Base backoff in ms; the delay before retry `n` is
     *   `initialBackoffMillis * 2^n` (1s, 2s, 4s, …).
     */
    fun downloadToFile(
        url: String,
        targetFile: File,
        onProgress: ((DownloadProgress) -> Unit)? = null,
        client: OkHttpClient = HttpClientProvider.client,
        maxRetries: Int = 2,
        initialBackoffMillis: Long = 1000L,
    ): File {
        require(maxRetries >= 0) { "maxRetries must be >= 0" }
        val totalAttempts = maxRetries + 1
        var lastError: Throwable? = null

        for (attempt in 0 until totalAttempts) {
            onProgress?.invoke(
                DownloadProgress(
                    progress = 0f,
                    bytesPerSecond = 0L,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                )
            )
            try {
                return downloadOnce(url, targetFile, onProgress, client)
            } catch (e: Http4xxException) {
                // 4xx is deterministic (auth, not-found, etc.); retrying cannot fix it.
                throw e
            } catch (e: EmptyBodyException) {
                // An empty body is also deterministic; retrying cannot fix it.
                throw e
            } catch (e: IOException) {
                // Transient network failure; retry after backoff.
                lastError = e
            } catch (e: Http5xxException) {
                // Transient server failure; retry after backoff.
                lastError = e
            }

            if (attempt < totalAttempts - 1) {
                val backoff = initialBackoffMillis * (1L shl attempt)
                try {
                    Thread.sleep(backoff)
                } catch (ie: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Download interrupted during backoff", ie)
                }
            }
        }

        throw IOException(
            "Download failed after $totalAttempts attempts: ${url}",
            lastError,
        )
    }

    private fun downloadOnce(
        url: String,
        targetFile: File,
        onProgress: ((DownloadProgress) -> Unit)?,
        client: OkHttpClient,
    ): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        try {
            if (response.code in 400..499) {
                throw Http4xxException("Download failed: HTTP ${response.code} ${response.message}")
            }
            if (response.code in 500..599) {
                throw Http5xxException("Download failed: HTTP ${response.code} ${response.message}")
            }
            check(response.isSuccessful) { "Download failed: HTTP ${response.code} ${response.message}" }
            val body = response.body ?: throw EmptyBodyException("No response body")
            val totalBytes = body.contentLength()
            if (totalBytes == 0L) throw EmptyBodyException("Empty response body")

            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastReported = -1f
                    var lastEmitNanos = System.nanoTime()
                    var lastEmitBytes = 0L
                    var smoothedBytesPerSecond = 0.0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        if (onProgress != null && totalBytes > 0) {
                            totalRead += bytesRead
                            val p = totalRead.toFloat() / totalBytes
                            if (p - lastReported >= 0.01f) {
                                val nowNanos = System.nanoTime()
                                val instantBytesPerSecond = instantBytesPerSecond(
                                    bytesSinceLast = totalRead - lastEmitBytes,
                                    elapsedNanos = nowNanos - lastEmitNanos,
                                )
                                smoothedBytesPerSecond =
                                    0.3 * instantBytesPerSecond + 0.7 * smoothedBytesPerSecond
                                lastEmitNanos = nowNanos
                                lastEmitBytes = totalRead
                                lastReported = p
                                onProgress(
                                    DownloadProgress(
                                        progress = p,
                                        bytesPerSecond = smoothedBytesPerSecond.toLong(),
                                        bytesDownloaded = totalRead,
                                        totalBytes = totalBytes,
                                    )
                                )
                            }
                        }
                    }
                    onProgress?.invoke(
                        DownloadProgress(
                            progress = 1f,
                            bytesPerSecond = 0L,
                            bytesDownloaded = totalRead,
                            totalBytes = totalBytes,
                        )
                    )
                }
            }
            if (targetFile.length() == 0L) throw EmptyBodyException("Downloaded file is empty")
            return targetFile
        } finally {
            response.close()
        }
    }

    private fun instantBytesPerSecond(bytesSinceLast: Long, elapsedNanos: Long): Double =
        if (elapsedNanos <= 0L) 0.0 else (bytesSinceLast * 1_000_000_000.0) / elapsedNanos

    /** Non-retriable: deterministic client error (HTTP 4xx). */
    private class Http4xxException(message: String) : IOException(message)

    /** Retriable: transient server error (HTTP 5xx). */
    private class Http5xxException(message: String) : IOException(message)

    /** Non-retriable: response body missing or empty. */
    private class EmptyBodyException(message: String) : IOException(message)
}
