package com.skiletro.wheelwitch.util

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/** Shared HTTP download utility used by both [RewindPackManager] and [MiiWadInstaller]. */
object FileDownloader {
    private const val DOWNLOAD_BUFFER = 262144

    /**
     * Downloads [url] to [targetFile]. Blocking — call from a background dispatcher.
     * Throws on non-2xx HTTP status or missing body.
     *
     * @param onProgress Optional callback with progress as 0..1 (throttled to ≥1% changes, always emits 1f).
     * @param client OkHttp client to use. Defaults to [HttpClientProvider.client];
     *   pass [HttpClientProvider.largeDownloadClient] for big payloads to
     *   avoid spurious 15s read timeouts.
     */
    fun downloadToFile(
        url: String,
        targetFile: File,
        onProgress: ((Float) -> Unit)? = null,
        client: OkHttpClient = HttpClientProvider.client,
    ): File {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        check(response.isSuccessful) { "Download failed: HTTP ${response.code} ${response.message}" }
        val body = requireNotNull(response.body) { "No response body" }
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER)
                var bytesRead: Int
                var totalRead = 0L
                var lastReported = -1f
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    if (onProgress != null && totalBytes > 0) {
                        totalRead += bytesRead
                        val p = totalRead.toFloat() / totalBytes
                        if (p - lastReported >= 0.01f) {
                            lastReported = p
                            onProgress(p)
                        }
                    }
                }
                onProgress?.invoke(1f)
            }
        }
        return targetFile
    }
}
