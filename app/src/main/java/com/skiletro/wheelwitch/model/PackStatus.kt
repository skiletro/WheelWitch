package com.skiletro.wheelwitch.model

sealed class PackStatus {
    data object NotInstalled : PackStatus()
    data class Installed(val version: SemVersion) : PackStatus()
    data class UpdateAvailable(
        val currentVersion: SemVersion,
        val latestVersion: SemVersion,
        val serverInfo: ServerInfo
    ) : PackStatus()

    data class UpToDate(val currentVersion: SemVersion, val latestVersion: SemVersion) : PackStatus()
}
