package com.skiletro.wheelwitch.util

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Base URL for fetching Mii face images by Base64-encoded RFL data. */
const val MII_IMAGE_BASE_URL = "https://mii-unsecure.ariankordi.net/miis/image.png"

/** Shared OkHttpClient singleton with 15s timeouts and redirect support. Used by all HTTP callers in the app. */
object HttpClientProvider {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
}
