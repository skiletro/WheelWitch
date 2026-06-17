package com.skiletro.wheelwitch.util

import okhttp3.Request
import java.io.File

object FileDownloader {
    private const val DOWNLOAD_BUFFER = 262144

    fun downloadToFile(
        url: String,
        targetFile: File,
        onProgress: ((Float) -> Unit)? = null,
    ): File {
        val request = Request.Builder().url(url).build()
        val response = HttpClientProvider.client.newCall(request).execute()
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
