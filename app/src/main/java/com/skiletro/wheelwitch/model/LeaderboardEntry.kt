package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable
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
    val root = JSONObject(jsonString)
    val data = root.getJSONArray("data")
    val entries = mutableListOf<LeaderboardEntry>()
    for (i in 0 until data.length()) {
        val obj = data.getJSONObject(i)
        entries.add(
            LeaderboardEntry(
                rank = obj.optInt("rank", 0),
                friendCode = obj.optString("friendCode", ""),
                name = obj.optString("name", "Unknown"),
                vr = obj.optInt("vr", 0),
                br = obj.optInt("br", 0),
                miiImageBase64 = if (!obj.isNull("miiImageBase64")) obj.optString("miiImageBase64", "").takeIf { it.isNotEmpty() } else null,
                country = if (!obj.isNull("country")) obj.optString("country", "").takeIf { it.isNotEmpty() } else null
            )
        )
    }
    return LeaderboardResponse(
        entries = entries,
        page = root.optInt("page", 1),
        hasMore = root.optBoolean("hasMore", false)
    )
}
