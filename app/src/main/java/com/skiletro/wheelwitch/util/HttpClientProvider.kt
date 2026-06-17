package com.skiletro.wheelwitch.util

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

const val MII_IMAGE_BASE_URL = "https://mii-unsecure.ariankordi.net/miis/image.png"

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
