package com.skiletro.wheelwitch.model

sealed class ProgressInfo {
    data class Checking(val message: String) : ProgressInfo()
    data class Downloading(val progress: Float, val message: String) : ProgressInfo()
    data class Extracting(val progress: Float) : ProgressInfo()
    data class ApplyingUpdate(
        val index: Int, val total: Int,
        val description: String, val progress: Float
    ) : ProgressInfo()
}
