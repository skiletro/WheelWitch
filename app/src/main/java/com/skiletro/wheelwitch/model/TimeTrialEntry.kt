package com.skiletro.wheelwitch.model

import org.json.JSONArray
import org.json.JSONObject

data class TimeTrialTrack(
    val id: String,
    val name: String
)

data class TimeTrialEntry(
    val rank: Int,
    val playerName: String,
    val friendCode: String,
    val time: String,
    val character: String?,
    val vehicle: String?
)

fun parseTracks(jsonString: String): List<TimeTrialTrack> {
    val root = JSONObject(jsonString)
    val arr = root.optJSONArray("tracks") ?: root.optJSONArray("data") ?: return emptyList()
    val tracks = mutableListOf<TimeTrialTrack>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        tracks.add(
            TimeTrialTrack(
                id = obj.optString("id", ""),
                name = obj.optString("name", "Unknown")
            )
        )
    }
    return tracks
}
