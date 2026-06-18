package com.skiletro.wheelwitch.model

import com.skiletro.wheelwitch.util.jsonArrayFromResponse

data class TimeTrialTrack(
    val id: String,
    val name: String
)

/**
 * One time-trial leaderboard entry.
 *
 * [time] is the finish time as the server returns it (typically
 * `"M:SS.mmm"`); the parser does not normalize it.
 */
data class TimeTrialEntry(
    val rank: Int,
    val playerName: String,
    val friendCode: String,
    val time: String,
    val character: String?,
    val vehicle: String?
)

/**
 * Parses the `/api/timetrial/tracks` JSON response into a list of
 * [TimeTrialTrack]. Accepts either a bare top-level array or an object
 * wrapping the array under `tracks` or `data`.
 */
fun parseTracks(jsonString: String): List<TimeTrialTrack> {
    val arr = jsonArrayFromResponse(jsonString, "tracks", "data") ?: return emptyList()
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
