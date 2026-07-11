package com.skiletro.wheelwitch.model

import kotlin.math.roundToLong

private const val W_VR = 0.60
private const val W_WIN = 0.15
private const val W_FIRSTS = 0.15
private const val W_DIST = 0.05
private const val W_DIST1ST = 0.05

private const val AH_VR = 100.0
private const val AH_WIN = 55.0
private const val AH_FIRSTS = 100.0
private const val AH_DIST = 100.0
private const val AH_DIST1ST = 100.0

private const val AL_VR = 5.0
private const val AL_WIN = 50.0
private const val AL_FIRSTS = 0.0
private const val AL_DIST = 0.0
private const val AL_DIST1ST = 0.0

private val M1 = W_VR * AH_VR + W_WIN * AH_WIN + W_FIRSTS * AH_FIRSTS +
  W_DIST * AH_DIST + W_DIST1ST * AH_DIST1ST
private val M2 = W_VR * AL_VR + W_WIN * AL_WIN + W_FIRSTS * AL_FIRSTS +
  W_DIST * AL_DIST + W_DIST1ST * AL_DIST1ST
private val ALPHA = 90.0 / (M1 - M2)
private val BETA = 100.0 - ALPHA * M1

private const val MIN_VS_MATCHES = 100
private const val MAX_VR_FANCY = 1_000_000.0
private const val MAX_VR_FLOAT = 10_000.0

val RANK_THRESH = doubleArrayOf(0.0, 24.0, 36.0, 48.0, 60.0, 72.0, 84.0, 94.0, 100.0)

val RANK_NAMES = arrayOf("E", "D", "C", "B", "A", "1star", "2star", "3star", "Crown")

data class LicenseStats(
  val vrPoints: Double,
  val vsWins: Int,
  val vsLosses: Int,
  val firsts: Int,
  val dist: Double,
  val dist1st: Double,
)

data class ScoreResult(
  val score: Double,
  val rank: Int,
  val vrNorm: Double,
  val winPct: Double,
  val firstsNorm: Double,
  val distNorm: Double,
  val dist1stNorm: Double,
  val totalVs: Int,
  val meetsRaceReq: Boolean,
)

data class StatNeed(
  val neededNorm: Double,
  val neededRaw: Double,
  val feasibility: String,
  val extraWins: Int? = null,
)

data class NextRankInfo(
  val threshold: Double?,
  val vr: StatNeed,
  val winPct: StatNeed,
  val firsts: StatNeed,
  val dist: StatNeed,
  val dist1st: StatNeed,
  val isMaxRank: Boolean = false,
)

private fun clamp(v: Double, lo: Double, hi: Double) = v.coerceIn(lo, hi)

private fun toInternalVr(vrPoints: Double): Double {
  val v = clamp(vrPoints.coerceAtLeast(0.0), 0.0, MAX_VR_FANCY).roundToLong().toDouble()
  return clamp(v / 100.0, 0.0, MAX_VR_FLOAT)
}

fun computeScore(stats: LicenseStats): ScoreResult {
  val totalVs = stats.vsWins + stats.vsLosses
  val winPct = if (totalVs > 0) 100.0 * stats.vsWins / totalVs else 45.0

  val vrInternal = toInternalVr(stats.vrPoints)
  val vrClamped = clamp(vrInternal, 0.0, 1000.0)
  val vrNorm = (vrClamped / 1000.0) * 100.0

  val firstsNorm = if (stats.firsts >= 2250) 100.0 else 100.0 * stats.firsts / 2250.0
  val distNorm = if (stats.dist >= 40000) 100.0 else 100.0 * stats.dist / 40000.0
  val dist1stNorm = if (stats.dist1st >= 10000) 100.0 else 100.0 * stats.dist1st / 10000.0

  val M = W_VR * vrNorm + W_WIN * winPct + W_FIRSTS * firstsNorm +
    W_DIST * distNorm + W_DIST1ST * dist1stNorm

  val meetsRaceReq = totalVs >= MIN_VS_MATCHES
  val score = if (meetsRaceReq) clamp(ALPHA * M + BETA, 0.0, 100.0) else 0.0
  val rank = if (meetsRaceReq) rankFromScore(score) else 0

  return ScoreResult(score, rank, vrNorm, winPct, firstsNorm, distNorm, dist1stNorm, totalVs, meetsRaceReq)
}

fun rankFromScore(score: Double): Int = when {
  score >= 100.0 -> 9
  score >= 94.0  -> 8
  score >= 84.0  -> 7
  score >= 72.0  -> 6
  score >= 60.0  -> 5
  score >= 48.0  -> 4
  score >= 36.0  -> 3
  score >= 24.0  -> 2
  else           -> 1
}

fun nextThreshold(rank: Int): Double? = RANK_THRESH.getOrNull(rank)

fun wouldBeScore(result: ScoreResult): Double {
  val M = W_VR * result.vrNorm + W_WIN * result.winPct + W_FIRSTS * result.firstsNorm +
    W_DIST * result.distNorm + W_DIST1ST * result.dist1stNorm
  return (ALPHA * M + BETA).coerceIn(0.0, 100.0)
}

fun wouldBeRank(result: ScoreResult): Int = rankFromScore(wouldBeScore(result))

fun computeNeeds(stats: LicenseStats): NextRankInfo {
  val cur = computeScore(stats)
  val thr = nextThreshold(cur.rank) ?: return NextRankInfo(
    threshold = null, isMaxRank = true,
    vr = StatNeed(0.0, 0.0, ""),
    winPct = StatNeed(0.0, 0.0, ""),
    firsts = StatNeed(0.0, 0.0, ""),
    dist = StatNeed(0.0, 0.0, ""),
    dist1st = StatNeed(0.0, 0.0, ""),
  )

  val mReq = (thr - BETA) / ALPHA

  fun otherSum(omit: String): Double {
    val parts = mapOf(
      "VR" to cur.vrNorm, "WIN" to cur.winPct,
      "FIRSTS" to cur.firstsNorm, "DIST" to cur.distNorm,
      "DIST1ST" to cur.dist1stNorm,
    )
    val weights = mapOf("VR" to W_VR, "WIN" to W_WIN, "FIRSTS" to W_FIRSTS,
      "DIST" to W_DIST, "DIST1ST" to W_DIST1ST)
    return parts.entries.filter { it.key != omit }.sumOf { (k, v) -> weights[k]!! * v }
  }

  fun solveNorm(omit: String): Double {
    val w = when (omit) {
      "VR" -> W_VR; "WIN" -> W_WIN; "FIRSTS" -> W_FIRSTS
      "DIST" -> W_DIST; "DIST1ST" -> W_DIST1ST; else -> 0.0
    }
    return (mReq - otherSum(omit)) / w
  }

  fun feas(normReq: Double) = when {
    normReq <= 100.0 -> "ok"
    normReq <= 110.0 -> "warn"
    else -> "infeasible"
  }

  val vrNormReq = solveNorm("VR")
  val vrRaw = clamp(Math.ceil(vrNormReq * 10.0), 0.0, 1000.0) * 100.0

  val winPctReq = solveNorm("WIN")
  val winPctRaw = clamp(Math.ceil(winPctReq * 100) / 100, 0.0, 100.0)
  val totalVs = stats.vsWins + stats.vsLosses
  val extraWins = if (totalVs > 0 && winPctRaw > cur.winPct) {
    val p = winPctRaw / 100.0
    if (p >= 1.0) null
    else {
      val x = Math.ceil((p * totalVs - stats.vsWins) / (1 - p)).toInt()
      if (x > 0) x else 0
    }
  } else null

  val firstsNormReq = solveNorm("FIRSTS")
  val firstsRaw = clamp(Math.ceil(firstsNormReq * 22.5), 0.0, 2250.0)

  val distNormReq = solveNorm("DIST")
  val distRaw = clamp(Math.ceil(distNormReq * 400.0), 0.0, 40000.0)

  val dist1stNormReq = solveNorm("DIST1ST")
  val dist1Raw = clamp(Math.ceil(dist1stNormReq * 100.0), 0.0, 10000.0)

  return NextRankInfo(
    threshold = thr,
    vr = StatNeed(vrNormReq, vrRaw.roundToLong().toDouble(), feas(vrNormReq)),
    winPct = StatNeed(winPctReq, winPctRaw, feas(winPctReq), extraWins),
    firsts = StatNeed(firstsNormReq, firstsRaw.roundToLong().toDouble(), feas(firstsNormReq)),
    dist = StatNeed(distNormReq, distRaw.roundToLong().toDouble(), feas(distNormReq)),
    dist1st = StatNeed(dist1stNormReq, dist1Raw.roundToLong().toDouble(), feas(dist1stNormReq)),
  )
}
