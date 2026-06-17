package com.skiletro.wheelwitch.model

data class ChangelogEntry(
    val version: String,
    val date: String,
    val changes: List<String>
)
