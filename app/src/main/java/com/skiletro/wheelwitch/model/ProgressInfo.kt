package com.skiletro.wheelwitch.model

/** Progress update emitted during install/update operations. */
sealed class ProgressInfo {
    /** Checking preconditions before starting. */
    data class Checking(val message: String) : ProgressInfo()

    /** Downloading a file; [progress] is 0..1, [bytesPerSecond] is the smoothed transfer rate, [bytesDownloaded] and [totalBytes] (0 if unknown) report raw transfer size. */
    data class Downloading(
        val progress: Float,
        val bytesPerSecond: Long,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val message: String
    ) : ProgressInfo()

    /** Extracting a zip archive; [progress] is 0..1. */
    data class Extracting(val progress: Float) : ProgressInfo()

    /** Applying one step of a multi-step incremental update. */
    data class ApplyingUpdate(
        val index: Int, val total: Int,
        val description: String, val progress: Float
    ) : ProgressInfo()
}
