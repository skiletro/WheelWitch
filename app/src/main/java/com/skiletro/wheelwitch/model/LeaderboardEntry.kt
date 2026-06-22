package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable

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
