package com.skiletro.wheelwitch.model

import androidx.compose.runtime.Immutable

/** Describes the current state of the Retro Rewind Pack on disk relative to the update server. */
@Immutable
sealed class PackStatus {
    /** No pack files found in storage. */
    data object NotInstalled : PackStatus()

    /** Pack is installed but the server could not be reached; version may be stale. */
    data class Installed(val version: SemVersion) : PackStatus()

    /**
     * Pack status could not be determined because the server was
     * unreachable. Distinct from [Installed] (verified-up-to-date) and
     * from a top-level UI error: the local install is still valid and
     * usable, but the user is shown a retry affordance because the
     * displayed version may be stale.
     */
    data class CheckFailed(val installedVersion: SemVersion?) : PackStatus()

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
