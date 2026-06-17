package com.skiletro.wheelwitch.model

import org.json.JSONObject

data class RaceStats(
    val totalRaces: Int,
    val totalPlayers: Int,
    val trackedSince: String?
)

fun parseRaceStats(jsonString: String): RaceStats {
    val root = JSONObject(jsonString)
    return RaceStats(
        totalRaces = root.optInt("totalRacesTracked", 0),
        totalPlayers = root.optInt("uniquePlayersCount", 0),
        trackedSince = if (!root.isNull("trackedSince")) root.optString("trackedSince", "").takeIf { it.isNotEmpty() } else null
    )
}
