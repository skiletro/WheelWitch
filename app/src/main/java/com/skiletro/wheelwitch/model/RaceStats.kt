package com.skiletro.wheelwitch.model

data class TrackStat(val name: String, val raceCount: Int)
data class NamedStat(val name: String, val raceCount: Int)
data class WinRateStat(val name: String, val raceCount: Int, val winCount: Int, val winRate: Double)
data class ActivePlayer(val name: String, val pid: String, val fc: String, val raceCount: Int)
data class DayStat(val dayName: String, val raceCount: Int)
data class HourStat(val hour: Int, val raceCount: Int)

/**
 * Global race statistics parsed from the RWFC `/api/racestats/global`
 * response. All list fields default to empty when the corresponding JSON
 * array is absent.
 *
 * [trackedSince] is an ISO-8601 timestamp string (e.g. "2024-01-15") for
 * the earliest date included in the stats, or null when the server omits it.
 *
 * [ActivePlayer.pid] is the player's persistent ID and [ActivePlayer.fc]
 * is their friend code.
 */
data class RaceStats(
    val totalRaces: Int,
    val totalPlayers: Int,
    val trackedSince: String?,
    val allPlayedTracks: List<TrackStat>,
    val topCharacters: List<NamedStat>,
    val topVehicles: List<NamedStat>,
    val topCombos: List<NamedStat>,
    val mostActivePlayers: List<ActivePlayer>,
    val racesByDayOfWeek: List<DayStat>,
    val racesByHour: List<HourStat>,
    val topCharactersByWinRate: List<WinRateStat>,
    val topVehiclesByWinRate: List<WinRateStat>,
    val topCombosByWinRate: List<WinRateStat>,
)
