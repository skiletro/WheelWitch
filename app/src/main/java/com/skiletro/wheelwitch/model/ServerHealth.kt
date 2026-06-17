package com.skiletro.wheelwitch.model

import org.json.JSONObject

data class HealthCheckItem(
    val status: String,
    val message: String?
)

data class ServerHealth(
    val status: String,
    val database: HealthCheckItem?,
    val postgresql: HealthCheckItem?,
    val retroWfcApi: HealthCheckItem?,
    val memory: MemoryInfo?
)

data class MemoryInfo(
    val status: String,
    val usagePercent: Double?,
    val used: String?,
    val total: String?
)

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
            val description = if (!check.isNull("description")) check.optString("description", "").takeIf { it.isNotEmpty() } else null

            when (name) {
                "LeaderboardDbContext" -> database = HealthCheckItem(status, description)
                "npgsql" -> postgresql = HealthCheckItem(status, description)
                "retro-wfc-api" -> retroWfcApi = HealthCheckItem(status, description)
                "memory" -> memoryInfo = parseMemoryFromDescription(status, description)
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

private fun normalizeStatus(status: String): String = when {
    status.equals("healthy", ignoreCase = true) -> "ok"
    status.equals("unhealthy", ignoreCase = true) -> "error"
    status.equals("degraded", ignoreCase = true) -> "degraded"
    else -> status.lowercase()
}

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
