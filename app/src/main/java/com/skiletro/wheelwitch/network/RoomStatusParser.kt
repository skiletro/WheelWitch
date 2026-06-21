package com.skiletro.wheelwitch.network

import com.skiletro.wheelwitch.model.MiiData
import com.skiletro.wheelwitch.model.Player
import com.skiletro.wheelwitch.model.Room
import org.json.JSONArray
import org.json.JSONObject

/** Parses the `rwfc.net/api/roomstatus` JSON response into a list of [Room]. */
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
                players = parsePlayers(obj.getJSONArray("players")),
                averageVR = obj.optInt("averageVR", 0),
                trackName = race?.optString("trackName")?.ifEmpty { null },
                roomType = obj.optString("roomType", "Unknown"),
                isPublic = obj.optBoolean("isPublic", false),
                isJoinable = obj.optBoolean("isJoinable", false)
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
