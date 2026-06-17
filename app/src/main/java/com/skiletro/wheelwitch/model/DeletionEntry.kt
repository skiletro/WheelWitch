package com.skiletro.wheelwitch.model

/** A file deletion step from the update server manifest. */
data class DeletionEntry(
    val version: SemVersion,
    val path: String
)
