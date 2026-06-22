package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.ActivePlayer
import com.skiletro.wheelwitch.model.DayStat
import com.skiletro.wheelwitch.model.HourStat
import com.skiletro.wheelwitch.model.NamedStat
import com.skiletro.wheelwitch.model.RaceStats
import com.skiletro.wheelwitch.model.TrackStat
import com.skiletro.wheelwitch.model.WinRateStat
import com.skiletro.wheelwitch.util.json.optNonEmptyString
import org.json.JSONArray
import org.json.JSONObject

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
