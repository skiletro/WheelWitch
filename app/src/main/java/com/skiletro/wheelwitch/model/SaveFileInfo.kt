package com.skiletro.wheelwitch.model

/** Parsed save file containing up to 4 license slots. */
data class SaveFileInfo(val licenses: List<LicenseInfo>)

/** Player data fetched from the RWFC leaderboard API for a single friend code. */
data class LeaderboardPlayerData(
    val vr: Int,
    val miiImageBase64: String?
)

/** One of 4 license slots parsed from `rksys.dat`, optionally enriched with leaderboard data. */
data class LicenseInfo(
    val slotIndex: Int,
    val exists: Boolean,
    val miiName: String? = null,
    val friendCode: String? = null,
    val vr: Int? = null,
    val raceWins: Int? = null,
    val raceLosses: Int? = null,
    val miiDataBase64: String? = null,
    val leaderboard: LeaderboardPlayerData? = null
)
