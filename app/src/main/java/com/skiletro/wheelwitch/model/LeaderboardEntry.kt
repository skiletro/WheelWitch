package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class LeaderboardEntry(
    val rank: Int,
    val friendCode: String,
    val name: String,
    val vr: Int,
    val br: Int,
    val miiImageBase64: String?,
    val country: String?
)

data class LeaderboardResponse(
    val entries: List<LeaderboardEntry>,
    val page: Int,
    val hasMore: Boolean
)

fun parseLeaderboardResponse(jsonString: String): LeaderboardResponse {
    val trimmed = jsonString.trim()
    val data: JSONArray
    var page = 1
    var hasMore = false

    if (trimmed.startsWith("[")) {
        data = JSONArray(trimmed)
    } else {
        val root = JSONObject(trimmed)
        data = root.optJSONArray("players")
            ?: root.optJSONArray("data")
            ?: root.optJSONArray("results")
            ?: root.optJSONArray("leaderboard")
            ?: root.optJSONArray("entries")
            ?: JSONArray()
        page = root.optInt("currentPage", root.optInt("page", 1))
        val totalPages = root.optInt("totalPages", root.optInt("total_pages", 1))
        hasMore = page < totalPages || root.optBoolean("hasMore", root.optBoolean("has_more", root.has("next")))
    }

    val entries = mutableListOf<LeaderboardEntry>()
    for (i in 0 until data.length()) {
        val obj = data.getJSONObject(i)
        entries.add(
            LeaderboardEntry(
                rank = obj.optInt("rank", 0),
                friendCode = obj.optString("friendCode", obj.optString("friend_code", obj.optString("fc", ""))),
                name = obj.optString("name", "Unknown"),
                vr = obj.optInt("vr", 0),
                br = obj.optInt("br", 0),
                miiImageBase64 = if (!obj.isNull("miiImageBase64")) obj.optString("miiImageBase64", "").takeIf { it.isNotEmpty() } else null,
                country = if (!obj.isNull("country")) obj.optString("country", "").takeIf { it.isNotEmpty() } else null
            )
        )
    }
    return LeaderboardResponse(entries = entries, page = page, hasMore = hasMore)
}
