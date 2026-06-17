package com.skiletro.wheelwitch.model

/** Parsed response from the update server containing latest version, update steps, and file deletions. */
data class ServerInfo(
    val latestVersion: SemVersion,
    val allUpdates: List<UpdateEntry>,
    val deletions: List<DeletionEntry>
)
