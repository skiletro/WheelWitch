package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.DeletionEntry
import com.skiletro.wheelwitch.model.LeaderboardPlayerData
import com.skiletro.wheelwitch.model.LeaderboardResponse
import com.skiletro.wheelwitch.model.RaceStats
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerHealth
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.TimeTrialTrack
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.model.parseHealthResponse
import com.skiletro.wheelwitch.model.parseLeaderboardResponse
import com.skiletro.wheelwitch.model.parseRaceStats
import com.skiletro.wheelwitch.model.parseRooms
import com.skiletro.wheelwitch.model.parseTracks
import com.skiletro.wheelwitch.util.HttpClientProvider
import okhttp3.Request
import org.json.JSONObject

/** Parses RetroRewindVersion.txt format: lines of "version url path description". Skips malformed lines. */
internal fun parseUpdatesText(text: String): List<UpdateEntry> {
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

/** Parses RetroRewindDelete.txt format: lines of "version path". Skips malformed lines. */
internal fun parseDeletionsText(text: String): List<DeletionEntry> {
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

/** Fetches update manifests, room status, leaderboard data, and VR multipliers from the RWFC network. */
object VersionFileParser {
    private const val RR_BASE = "https://update.rwfc.net/"
    private const val VERSION_URL = "${RR_BASE}RetroRewind/RetroRewindVersion.txt"
    private const val DELETE_URL = "${RR_BASE}RetroRewind/RetroRewindDelete.txt"
    private const val FULL_ZIP_URL = "${RR_BASE}RetroRewind/zip/RetroRewind.zip"
    private const val RWFC_API = "https://rwfc.net"
    private const val ROOM_STATUS_URL = "$RWFC_API/api/roomstatus"
    private const val MULTIPLIER_URL = "https://rwfc.net/updates/RetroRewind/multiplier.txt"
    private const val LEADERBOARD_URL = "$RWFC_API/api/leaderboard"
    private const val HEALTH_URL = "$RWFC_API/api/health"
    private const val HEALTH_READY_URL = "$RWFC_API/api/health/ready"
    private const val RACE_STATS_URL = "$RWFC_API/api/racestats/global"
    private const val TIME_TRIAL_TRACKS_URL = "$RWFC_API/api/timetrial/tracks"

    private val httpClient get() = HttpClientProvider.client

    /** Fetches the full update manifest: latest version, all update steps, and file deletions. */
    fun fetchServerInfo(): Result<ServerInfo> = runCatching {
        val updates = fetchUpdates()
        if (updates.isEmpty()) error("No versions found on server")
        val deletions = fetchDeletions()
        val latestEntry = updates.maxBy { it.version }
        ServerInfo(latestVersion = latestEntry.version, allUpdates = updates, deletions = deletions)
    }

    private fun fetchUpdates() = parseUpdatesText(fetchUrl(VERSION_URL))

    private fun fetchDeletions() = parseDeletionsText(fetchUrl(DELETE_URL))

    /** Returns the URL for the full Retro Rewind zip download. */
    fun getFullZipUrl(): String = FULL_ZIP_URL

    /** Fetches the list of active multiplayer rooms from the RWFC API. */
    fun fetchRooms(): Result<List<Room>> = runCatching {
        val json = fetchUrl(ROOM_STATUS_URL)
        parseRooms(json)
    }

    /** Fetches the current VR multiplier from the update server (e.g. "2.0" for 2x weekends). */
    fun fetchVrMultiplier(): Result<Float> = runCatching {
        fetchUrl(MULTIPLIER_URL).trim().toFloat()
    }

    /** Fetches a single page from the paginated VR leaderboard. */
    fun fetchLeaderboard(page: Int = 1, limit: Int = 50): Result<LeaderboardResponse> = runCatching {
        val url = "$LEADERBOARD_URL?page=$page&limit=$limit"
        val json = fetchUrl(url)
        parseLeaderboardResponse(json)
    }

    /** Checks whether the server is fully ready (all subsystems pass). Returns true if 200. */
    fun fetchHealthReady(): Result<Boolean> = runCatching {
        val request = Request.Builder().url(HEALTH_READY_URL).build()
        httpClient.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }

    /** Fetches the full server health report. */
    fun fetchHealth(): Result<ServerHealth> = runCatching {
        val json = fetchUrl(HEALTH_URL)
        parseHealthResponse(json)
    }

    /** Fetches global race statistics. */
    fun fetchGlobalRaceStats(): Result<RaceStats> = runCatching {
        val json = fetchUrl(RACE_STATS_URL)
        parseRaceStats(json)
    }

    /** Fetches the list of time trial tracks. */
    fun fetchTracks(): Result<List<TimeTrialTrack>> = runCatching {
        val json = fetchUrl(TIME_TRIAL_TRACKS_URL)
        parseTracks(json)
    }

    /** Fetches leaderboard data (VR, Mii image) for a given friend code. */
    fun fetchPlayerLeaderboard(friendCode: String): Result<LeaderboardPlayerData> = runCatching {
        val url = "https://rwfc.net/api/leaderboard/player/$friendCode/"
        val json = fetchUrl(url)
        val root = JSONObject(json)
        LeaderboardPlayerData(
            vr = root.optInt("vr", 0),
            miiImageBase64 = root.optString("miiImageBase64", "").takeIf { it.isNotEmpty() }
        )
    }

    /** Blocking HTTP GET. Throws on non-2xx or empty body. */
    private fun fetchUrl(urlString: String): String {
        val request = Request.Builder().url(urlString).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Empty response from $urlString")
            if (!response.isSuccessful) error("$urlString returned ${response.code}: $body")
            return body
        }
    }
}
