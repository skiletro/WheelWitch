package com.skiletro.wheelwitch.model

import org.json.JSONObject

sealed interface ServerConnectivity {
    data object Online : ServerConnectivity
    data object Offline : ServerConnectivity
    data object NoInternet : ServerConnectivity
    data object Unknown : ServerConnectivity
}

fun parsePlayerCount(jsonString: String): Int {
    val root = JSONObject(jsonString)
    val rooms = root.getJSONArray("rooms")
    var count = 0
    for (i in 0 until rooms.length()) {
        count += rooms.getJSONObject(i).getJSONArray("players").length()
    }
    return count
}
