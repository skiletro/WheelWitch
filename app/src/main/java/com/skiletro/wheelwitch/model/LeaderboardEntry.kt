package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.util.jsonArrayFromResponse
import com.skiletro.wheelwitch.util.optAnyString
import com.skiletro.wheelwitch.util.optNonEmptyString
import org.json.JSONObject

/**
 * One row of the VR leaderboard.
 *
 * [miiImageBase64] and [miiData] are two optional Mii representations
 * returned by the API: [miiImageBase64] is a pre-rendered PNG image, and
 * [miiData] is the raw Base64-encoded RFL (Mii binary) payload that the
 * client can re-render via the Mii image service. Either may be null.
 */
@Immutable
data class LeaderboardEntry(
    val rank: Int,
    val friendCode: String,
    val name: String,
    val vr: Int,
    val miiImageBase64: String?,
    val miiData: String?
)

data class LeaderboardResponse(
    val entries: List<LeaderboardEntry>,
    val page: Int,
    val hasMore: Boolean
)

/**
 * Parses a `/api/leaderboard` JSON response into [LeaderboardResponse].
 *
 * The API has historically returned the entries array under several
 * field names (`players`, `data`, `results`, `leaderboard`, `entries`)
 * and pagination under camelCase, snake_case, or a `next` link. All
 * known variants are tolerated; a bare top-level array is also accepted.
 */
fun parseLeaderboardResponse(jsonString: String): LeaderboardResponse {
    val data = jsonArrayFromResponse(
        jsonString,
        "players", "data", "results", "leaderboard", "entries",
    ) ?: org.json.JSONArray()

    val page: Int
    val hasMore: Boolean
    if (jsonString.trim().startsWith("[")) {
        page = 1
        hasMore = false
    } else {
        val root = JSONObject(jsonString)
        page = root.optInt("currentPage", root.optInt("page", 1))
        val totalPages = root.optInt("totalPages", root.optInt("total_pages", 1))
        hasMore = page < totalPages ||
                root.optBoolean("hasMore", root.optBoolean("has_more", root.has("next")))
    }

    val entries = (0 until data.length()).map { i ->
        val obj = data.getJSONObject(i)
        LeaderboardEntry(
            rank = obj.optInt("rank", 0),
            friendCode = obj.optAnyString("friendCode", "friend_code", "fc") ?: "",
            name = obj.optString("name", "Unknown"),
            vr = obj.optInt("vr", 0),
            miiImageBase64 = obj.optNonEmptyString("miiImageBase64"),
            miiData = obj.optNonEmptyString("miiData"),
        )
    }
    return LeaderboardResponse(entries = entries, page = page, hasMore = hasMore)
}
