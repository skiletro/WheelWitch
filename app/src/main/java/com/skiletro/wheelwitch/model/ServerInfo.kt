package com.skiletro.wheelwitch.model

data class ServerInfo(
    val latestVersion: SemVersion,
    val allUpdates: List<UpdateEntry>,
    val deletions: List<DeletionEntry>
)
