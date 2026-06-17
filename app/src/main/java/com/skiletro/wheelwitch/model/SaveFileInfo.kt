package com.skiletro.wheelwitch.model

data class SaveFileInfo(val licenses: List<LicenseInfo>)

data class LicenseInfo(
    val slotIndex: Int,
    val exists: Boolean,
    val miiName: String? = null,
    val friendCode: String? = null,
    val vr: Int? = null,
    val br: Int? = null,
    val raceWins: Int? = null,
    val raceLosses: Int? = null,
    val battleWins: Int? = null,
    val battleLosses: Int? = null,
    val miiDataBase64: String? = null
)
