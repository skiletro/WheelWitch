package com.skiletro.wheelwitch.model

data class UpdateEntry(
    val version: SemVersion,
    val url: String,
    val path: String,
    val description: String
)
