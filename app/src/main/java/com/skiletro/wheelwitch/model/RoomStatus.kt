package com.skiletro.wheelwitch.model

import org.json.JSONObject

data class RoomStatusResponse(
    val rooms: List<Room>,
) {
    fun totalPlayerCount(): Int = rooms.sumOf { it.players.size }
}

data class Room(
    val id: String,
    val type: String,
    val players: List<Player>,
)

data class Player(
    val pid: String,
    val name: String,
)

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
