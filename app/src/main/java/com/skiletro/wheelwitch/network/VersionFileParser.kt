package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.DeletionEntry
import com.skiletro.wheelwitch.model.LeaderboardPlayerData
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.model.parseRooms
import com.skiletro.wheelwitch.util.HttpClientProvider
import okhttp3.Request
import org.json.JSONObject

object VersionFileParser {
    private const val RR_BASE = "https://update.rwfc.net/"
    private const val VERSION_URL = "${RR_BASE}RetroRewind/RetroRewindVersion.txt"
    private const val DELETE_URL = "${RR_BASE}RetroRewind/RetroRewindDelete.txt"
    private const val FULL_ZIP_URL = "${RR_BASE}RetroRewind/zip/RetroRewind.zip"
    private const val ROOM_STATUS_URL = "https://rwfc.net/api/roomstatus"
    private const val MULTIPLIER_URL = "https://rwfc.net/updates/RetroRewind/multiplier.txt"

    private val httpClient get() = HttpClientProvider.client

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

    private fun fetchDeletions(): List<DeletionEntry> {
        val text = fetchUrl(DELETE_URL)
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val version = SemVersion.parse(parts[0].trim()) ?: return@mapNotNull null
                DeletionEntry(version, parts[1].trim())
            }
    }

    fun getFullZipUrl(): String = FULL_ZIP_URL

    fun fetchRooms(): Result<List<Room>> = runCatching {
        val json = fetchUrl(ROOM_STATUS_URL)
        parseRooms(json)
    }

    fun fetchVrMultiplier(): Result<Float> = runCatching {
        fetchUrl(MULTIPLIER_URL).trim().toFloat()
    }

    fun fetchPlayerLeaderboard(friendCode: String): Result<LeaderboardPlayerData> = runCatching {
        val url = "https://rwfc.net/api/leaderboard/player/$friendCode/"
        val json = fetchUrl(url)
        val root = JSONObject(json)
        LeaderboardPlayerData(
            vr = root.optInt("vr", 0),
            miiImageBase64 = root.optString("miiImageBase64", "").takeIf { it.isNotEmpty() }
        )
    }

    private fun fetchUrl(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty response from $urlString")
            if (!response.isSuccessful) error("$urlString returned ${response.code}: $body")
            return body
        }
    }
}
