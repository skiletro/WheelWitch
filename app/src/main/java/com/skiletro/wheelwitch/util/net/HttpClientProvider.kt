package com.skiletro.wheelwitch.util.net

import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared OkHttpClient singletons.
 *
 * [client] has 15s timeouts and is used for metadata-style requests
 * (rooms, leaderboard, server health, Mii face fetches, etc.). The
 * [Dispatcher] is capped at [MAX_REQUESTS] /
 * [MAX_REQUESTS_PER_HOST] so a fan-out of Mii image requests (up to
 * 50 leaderboard rows × 4 retries) cannot starve the rest of the
 * app's HTTP traffic.
 *
 * [largeDownloadClient] has 60s timeouts and is used for large payload
 * downloads (e.g. the multi-MB Retro Rewind pack zip and the Mii Channel
 * WAD zip). The longer timeouts prevent spurious read timeouts on slow
 * connections. [com.skiletro.wheelwitch.util.io.FileDownloader] clones
 * this client's dispatcher per parallel download so chunk workers
 * don't fight over the shared dispatcher; the cap here only applies
 * to the single-stream fallback.
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
            .dispatcher(
                Dispatcher().apply {
                    maxRequests = MAX_REQUESTS
                    maxRequestsPerHost = MAX_REQUESTS_PER_HOST
                }
            )
            .build()

    private const val MAX_REQUESTS: Int = 16
    private const val MAX_REQUESTS_PER_HOST: Int = 8
}
