package com.skiletro.wheelwitch.model

import org.json.JSONArray
import org.json.JSONObject

sealed interface ServerConnectivity {
    data object Online : ServerConnectivity
    data object Offline : ServerConnectivity
    data object NoInternet : ServerConnectivity
    data object Unknown : ServerConnectivity
}

data class Room(
    val id: String,
    val type: String,
    val host: String,
    val players: List<Player>,
    val averageVR: Int,
    val trackName: String?,
    val roomType: String,
    val isPublic: Boolean,
    val isJoinable: Boolean,
    val isSuspended: Boolean
)

data class MiiData(
    val data: String,
    val name: String
)

data class Player(
    val name: String,
    val friendCode: String,
    val vr: Int,
    val br: Int,
    val isOpenHost: Boolean,
    val mii: MiiData?
)

fun parseRooms(jsonString: String): List<Room> {
    val root = JSONObject(jsonString)
    val arr = root.getJSONArray("rooms")
    val rooms = mutableListOf<Room>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val race = obj.optJSONObject("race")
        rooms.add(
            Room(
                id = obj.getString("id"),
                type = obj.getString("type"),
                host = obj.getString("host"),
                players = parsePlayers(obj.getJSONArray("players")),
                averageVR = obj.optInt("averageVR", 0),
                trackName = race?.optString("trackName")?.ifEmpty { null },
                roomType = obj.optString("roomType", "Unknown"),
                isPublic = obj.optBoolean("isPublic", false),
                isJoinable = obj.optBoolean("isJoinable", false),
                isSuspended = obj.optBoolean("isSuspended", false)
            )
        )
    }
    return rooms
}

private fun parsePlayers(arr: JSONArray): List<Player> {
    val players = mutableListOf<Player>()
    for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val miiObj = obj.optJSONObject("mii")
        players.add(
            Player(
                name = obj.optString("name", "Unknown"),
                friendCode = obj.optString("friendCode", ""),
                vr = obj.optInt("vr", 0),
                br = obj.optInt("br", 0),
                isOpenHost = obj.optBoolean("isOpenHost", false),
                mii = if (miiObj != null) MiiData(
                    data = miiObj.optString("data", ""),
                    name = miiObj.optString("name", "")
                ) else null
            )
        )
    }
    return players
}


