package com.skiletro.wheelwitch.model

import org.json.JSONObject

data class RaceStats(
    val totalRaces: Int,
    val totalPlayers: Int,
    val totalTimePlayed: String?,
    val averagePlayersPerRace: Double?
)

fun parseRaceStats(jsonString: String): RaceStats {
    val root = JSONObject(jsonString)
    return RaceStats(
        totalRaces = root.optInt("totalRaces", 0),
        totalPlayers = root.optInt("totalPlayers", 0),
        totalTimePlayed = if (!root.isNull("totalTimePlayed")) root.optString("totalTimePlayed", "").takeIf { it.isNotEmpty() } else null,
        averagePlayersPerRace = if (root.has("averagePlayersPerRace")) {
            root.optDouble("averagePlayersPerRace", -1.0).let { if (it >= 0) it else null }
        } else null
    )
}
