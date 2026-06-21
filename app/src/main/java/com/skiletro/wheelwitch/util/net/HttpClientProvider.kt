package com.skiletro.wheelwitch.util.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient singletons.
 *
 * [client] has 15s timeouts and is used for metadata-style requests
 * (rooms, leaderboard, server health, Mii face fetches, etc.).
 *
 * [largeDownloadClient] has 60s timeouts and is used for large payload
 * downloads (e.g. the multi-MB Retro Rewind pack zip and the Mii Channel
 * WAD zip). The longer timeouts prevent spurious read timeouts on slow
 * connections.
 */
object HttpClientProvider {
    val client: OkHttpClient by lazy { buildClient(readTimeoutSeconds = 15) }

    val largeDownloadClient: OkHttpClient by lazy { buildClient(readTimeoutSeconds = 60) }

    private fun buildClient(readTimeoutSeconds: Long): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
}
