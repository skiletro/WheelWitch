package com.skiletro.wheelwitch.model

/** A single version entry from the Retro Rewind wiki changelog. */
data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)
