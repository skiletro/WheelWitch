package com.skiletro.wheelwitch.model

import com.skiletro.wheelwitch.util.optNonEmptyString
import org.json.JSONArray
import org.json.JSONObject

data class TrackStat(val name: String, val raceCount: Int)
data class NamedStat(val name: String, val raceCount: Int)
data class WinRateStat(val name: String, val raceCount: Int, val winCount: Int, val winRate: Double)
data class ActivePlayer(val name: String, val pid: String, val fc: String, val raceCount: Int)
data class DayStat(val dayName: String, val raceCount: Int)
data class HourStat(val hour: Int, val raceCount: Int)

/**
 * Global race statistics parsed from the RWFC `/api/racestats/global`
 * response. All list fields default to empty when the corresponding JSON
 * array is absent.
 *
 * [trackedSince] is an ISO-8601 timestamp string (e.g. "2024-01-15") for
 * the earliest date included in the stats, or null when the server omits it.
 *
 * [ActivePlayer.pid] is the player's persistent ID and [ActivePlayer.fc]
 * is their friend code.
 */
data class RaceStats(
    val totalRaces: Int,
    val totalPlayers: Int,
    val trackedSince: String?,
    val allPlayedTracks: List<TrackStat>,
    val topCharacters: List<NamedStat>,
    val topVehicles: List<NamedStat>,
    val topCombos: List<NamedStat>,
    val mostActivePlayers: List<ActivePlayer>,
    val racesByDayOfWeek: List<DayStat>,
    val racesByHour: List<HourStat>,
    val topCharactersByWinRate: List<WinRateStat>,
    val topVehiclesByWinRate: List<WinRateStat>,
    val topCombosByWinRate: List<WinRateStat>,
)

/** Parses the `/api/racestats/global` JSON response into [RaceStats]; missing or null fields default to empty/0. */
fun parseRaceStats(jsonString: String): RaceStats {
    val root = JSONObject(jsonString)
    return RaceStats(
        totalRaces = root.optInt("totalRacesTracked", 0),
        totalPlayers = root.optInt("uniquePlayersCount", 0),
        trackedSince = root.optNonEmptyString("trackedSince"),
        allPlayedTracks = root.optJSONArray("allPlayedTracks")?.let { parseTrackStats(it) }
            ?: emptyList(),
        topCharacters = root.optJSONArray("topCharacters")?.let { parseNamedStats(it) }
            ?: emptyList(),
        topVehicles = root.optJSONArray("topVehicles")?.let { parseNamedStats(it) } ?: emptyList(),
        topCombos = root.optJSONArray("topCombos")?.let { parseNamedStats(it) } ?: emptyList(),
        mostActivePlayers = root.optJSONArray("mostActivePlayers")?.let { parseActivePlayers(it) }
            ?: emptyList(),
        racesByDayOfWeek = root.optJSONArray("racesByDayOfWeek")?.let { parseDayStats(it) }
            ?: emptyList(),
        racesByHour = root.optJSONArray("racesByHour")?.let { parseHourStats(it) } ?: emptyList(),
        topCharactersByWinRate = root.optJSONArray("topCharactersByWinRate")
            ?.let { parseWinRateStats(it) } ?: emptyList(),
        topVehiclesByWinRate = root.optJSONArray("topVehiclesByWinRate")
            ?.let { parseWinRateStats(it) } ?: emptyList(),
        topCombosByWinRate = root.optJSONArray("topCombosByWinRate")?.let { parseWinRateStats(it) }
            ?: emptyList(),
    )
}

private fun parseTrackStats(arr: JSONArray): List<TrackStat> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        TrackStat(
            name = obj.optString("trackName", ""),
            raceCount = obj.optInt("raceCount", 0)
        )
    }
}

private fun parseNamedStats(arr: JSONArray): List<NamedStat> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        NamedStat(
            name = obj.optString("name", ""),
            raceCount = obj.optInt("raceCount", 0)
        )
    }
}

private fun parseWinRateStats(arr: JSONArray): List<WinRateStat> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        WinRateStat(
            name = obj.optString("name", ""),
            raceCount = obj.optInt("raceCount", 0),
            winCount = obj.optInt("winCount", 0),
            winRate = obj.optDouble("winRate", 0.0)
        )
    }
}

private fun parseActivePlayers(arr: JSONArray): List<ActivePlayer> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        ActivePlayer(
            name = obj.optString("name", ""),
            pid = obj.optString("pid", ""),
            fc = obj.optString("fc", ""),
            raceCount = obj.optInt("raceCount", 0)
        )
    }
}

private fun parseDayStats(arr: JSONArray): List<DayStat> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        DayStat(
            dayName = obj.optString("dayName", ""),
            raceCount = obj.optInt("raceCount", 0)
        )
    }
}

private fun parseHourStats(arr: JSONArray): List<HourStat> {
    return (0 until arr.length()).map { i ->
        val obj = arr.getJSONObject(i)
        HourStat(
            hour = obj.optInt("hour", 0),
            raceCount = obj.optInt("raceCount", 0)
        )
    }
}
