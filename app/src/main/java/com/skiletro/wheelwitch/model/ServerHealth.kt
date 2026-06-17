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
    val checks = root.optJSONObject("checks")
    return ServerHealth(
        status = root.optString("status", "unknown"),
        database = checks?.optJSONObject("database")?.let { parseCheckItem(it) },
        postgresql = checks?.optJSONObject("postgresql")?.let { parseCheckItem(it) },
        retroWfcApi = checks?.optJSONObject("retro_wfc_api")?.let { parseCheckItem(it) },
        memory = checks?.optJSONObject("memory")?.let { parseMemoryInfo(it) }
    )
}

private fun parseCheckItem(obj: JSONObject) = HealthCheckItem(
    status = obj.optString("status", "unknown"),
    message = if (!obj.isNull("message")) obj.optString("message", "").takeIf { it.isNotEmpty() } else null
)

private fun parseMemoryInfo(obj: JSONObject) = MemoryInfo(
    status = obj.optString("status", "unknown"),
    usagePercent = if (obj.has("usage")) obj.optDouble("usage", -1.0).let { if (it >= 0) it else null } else null,
    used = if (!obj.isNull("used")) obj.optString("used", "").takeIf { it.isNotEmpty() } else null,
    total = if (!obj.isNull("total")) obj.optString("total", "").takeIf { it.isNotEmpty() } else null
)
