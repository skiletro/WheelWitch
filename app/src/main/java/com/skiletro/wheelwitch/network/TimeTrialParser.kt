package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.TimeTrialTrack
import com.skiletro.wheelwitch.util.json.jsonArrayFromResponse

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
