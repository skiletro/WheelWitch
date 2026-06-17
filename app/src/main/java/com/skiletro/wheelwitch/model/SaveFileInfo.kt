package com.skiletro.wheelwitch.model

/** Parsed save file containing up to 4 license slots. */
data class SaveFileInfo(val licenses: List<LicenseInfo>)

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
    val leaderboardVr: Int? = null,
    val leaderboardMiiImageBase64: String? = null
)
