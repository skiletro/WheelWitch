package com.skiletro.wheelwitch.model

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
