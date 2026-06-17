package com.skiletro.wheelwitch.util

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Base URL for fetching Mii face images by Base64-encoded RFL data. */
const val MII_IMAGE_BASE_URL = "https://mii-unsecure.ariankordi.net/miis/image.png"

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
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val largeDownloadClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
