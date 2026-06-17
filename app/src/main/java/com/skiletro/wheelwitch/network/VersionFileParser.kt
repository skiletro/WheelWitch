package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.DeletionEntry
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.model.parsePlayerCount
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object VersionFileParser {
    private const val RR_BASE = "https://update.rwfc.net/"
    private const val VERSION_URL = "${RR_BASE}RetroRewind/RetroRewindVersion.txt"
    private const val DELETE_URL = "${RR_BASE}RetroRewind/RetroRewindDelete.txt"
    private const val FULL_ZIP_URL = "${RR_BASE}RetroRewind/zip/RetroRewind.zip"
    private const val ROOM_STATUS_URL = "https://rwfc.net/api/roomstatus"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun fetchServerInfo(): Result<ServerInfo> = runCatching {
        val updates = fetchUpdates()
        if (updates.isEmpty()) error("No versions found on server")
        val deletions = fetchDeletions()
        val latestEntry = updates.maxBy { it.version }
        ServerInfo(latestVersion = latestEntry.version, allUpdates = updates, deletions = deletions)
    }

    private fun fetchUpdates(): List<UpdateEntry> {
        val text = fetchUrl(VERSION_URL)
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val version = parts[0].trim()
                val url = parts[1].trim()
                val path = parts[2].trim()
                val description = parts[3].trim()
                val parsed = SemVersion.parse(version) ?: return@mapNotNull null
                UpdateEntry(parsed, url, path, description)
            }
    }

    private fun fetchDeletions(): List<DeletionEntry> = runCatching {
        val text = fetchUrl(DELETE_URL)
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val version = SemVersion.parse(parts[0].trim()) ?: return@mapNotNull null
                DeletionEntry(version, parts[1].trim())
            }
    }.getOrDefault(emptyList())

    fun getFullZipUrl(): String = FULL_ZIP_URL

    fun fetchPlayerCount(): Result<Int> = runCatching {
        val json = fetchUrl(ROOM_STATUS_URL)
        parsePlayerCount(json)
    }

    fun okHttpClient(): OkHttpClient = client

    private fun fetchUrl(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        client.newCall(request).execute().use { response ->
            return response.body?.string() ?: error("Empty response from $urlString")
        }
    }
}
