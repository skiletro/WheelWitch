package com.skiletro.wheelwitch.model

import com.skiletro.wheelwitch.util.optNonEmptyString
import org.json.JSONObject

/**
 * One named check in the `/api/health` response.
 *
 * [status] is normalized to one of "ok", "error", or "degraded" by
 * [normalizeStatus]; [message] is the raw description, if any.
 */
data class HealthCheckItem(
    val status: String,
    val message: String?
)

/**
 * Parsed `/api/health` response. Each subsystem field is null when the
 * corresponding check is absent from the server response.
 */
data class ServerHealth(
    val status: String,
    val database: HealthCheckItem?,
    val postgresql: HealthCheckItem?,
    val retroWfcApi: HealthCheckItem?,
    val memory: MemoryInfo?
)

/**
 * Memory subsystem check.
 *
 * The RWFC API embeds memory usage in the check's free-text description
 * rather than a structured field, so only [used] is populated by
 * [parseMemoryFromDescription]. [usagePercent] and [total] are reserved
 * for future API support and are currently always null.
 */
data class MemoryInfo(
    val status: String,
    val usagePercent: Double?,
    val used: String?,
    val total: String?
)

/** Health-check `name` values used by the RWFC API. */
private const val CHECK_NAME_DATABASE = "LeaderboardDbContext"
private const val CHECK_NAME_POSTGRESQL = "npgsql"
private const val CHECK_NAME_RETRO_WFC_API = "retro-wfc-api"
private const val CHECK_NAME_MEMORY = "memory"

/** Parses the `/api/health` JSON response into [ServerHealth]; unknown check names are ignored. */
fun parseHealthResponse(jsonString: String): ServerHealth {
    val root = JSONObject(jsonString)
    val rawStatus = root.optString("status", "unknown")
    val checksArray = root.optJSONArray("checks")

    var database: HealthCheckItem? = null
    var postgresql: HealthCheckItem? = null
    var retroWfcApi: HealthCheckItem? = null
    var memoryInfo: MemoryInfo? = null

    if (checksArray != null) {
        for (i in 0 until checksArray.length()) {
            val check = checksArray.getJSONObject(i)
            val name = check.optString("name", "")
            val status = normalizeStatus(check.optString("status", "unknown"))
            val description = check.optNonEmptyString("description")

            when (name) {
                CHECK_NAME_DATABASE -> database = HealthCheckItem(status, description)
                CHECK_NAME_POSTGRESQL -> postgresql = HealthCheckItem(status, description)
                CHECK_NAME_RETRO_WFC_API -> retroWfcApi = HealthCheckItem(status, description)
                CHECK_NAME_MEMORY -> memoryInfo = parseMemoryFromDescription(status, description)
            }
        }
    }

    return ServerHealth(
        status = normalizeStatus(rawStatus),
        database = database,
        postgresql = postgresql,
        retroWfcApi = retroWfcApi,
        memory = memoryInfo
    )
}

/**
 * Normalizes the API's status vocabulary to the app's internal set:
 * "healthy" -> "ok", "unhealthy" -> "error", "degraded" -> "degraded".
 * Anything else is lowercased and passed through.
 */
private fun normalizeStatus(status: String): String = when {
    status.equals("healthy", ignoreCase = true) -> "ok"
    status.equals("unhealthy", ignoreCase = true) -> "error"
    status.equals("degraded", ignoreCase = true) -> "degraded"
    else -> status.lowercase()
}

/**
 * Extracts the "Memory usage: NNN UNIT" substring out of the check's
 * free-text [description] (the API does not return a structured memory
 * field). Returns a [MemoryInfo] with [MemoryInfo.used] set to
 * "NNNUNIT" (e.g. "256MB") on a match, or null when the description is
 * absent or does not contain the pattern.
 */
private fun parseMemoryFromDescription(status: String, description: String?): MemoryInfo {
    val used = description?.let { desc ->
        val regex = Regex("""Memory usage:\s*(\d+)\s*(MB|GB|KB)""", RegexOption.IGNORE_CASE)
        regex.find(desc)?.let { match ->
            "${match.groupValues[1]}${match.groupValues[2]}"
        }
    }
    return MemoryInfo(
        status = status,
        usagePercent = null,
        used = used,
        total = null
    )
}
