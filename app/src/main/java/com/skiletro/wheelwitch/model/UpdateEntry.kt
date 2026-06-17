package com.skiletro.wheelwitch.model

/** A single incremental update step from the update server manifest. */
data class UpdateEntry(
    val version: SemVersion,
    val url: String,
    val path: String,
    val description: String
)
