package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable

/** Describes the current state of the Retro Rewind Pack on disk relative to the update server. */
@Immutable
sealed class PackStatus {
    /** No pack files found in storage. */
    data object NotInstalled : PackStatus()

    /** Pack is installed but the server could not be reached; version may be stale. */
    data class Installed(val version: SemVersion) : PackStatus()

    /** A newer version is available on the server. */
    data class UpdateAvailable(
        val currentVersion: SemVersion,
        val latestVersion: SemVersion,
        val serverInfo: ServerInfo
    ) : PackStatus()

    /** Pack matches the latest server version. */
    data class UpToDate(val currentVersion: SemVersion, val latestVersion: SemVersion) :
        PackStatus()
}
