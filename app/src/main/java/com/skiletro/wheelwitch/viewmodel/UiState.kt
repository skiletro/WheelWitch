package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.model.PackStatus

/**
 * High-level state machine for the pack install / update flow.
 *
 * The lifecycle is:
 * `NoStorage -> Checking -> Downloading/Extracting/ApplyingUpdate -> Ready/Error`
 *
 * `Checking` recurs throughout the flow (the manager emits
 * [ProgressInfo.Checking] during deletion and download phases too), so
 * the diagram above is simplified.
 */
@Immutable
sealed class UiState {
    /** No storage folder has been picked yet. */
    data object NoStorage : UiState()

    /** A status check or install/update finished; [status] is the current pack state. */
    data class Ready(val status: PackStatus) : UiState()

    /** A status check or intermediate install phase is in progress. */
    data object Checking : UiState()

    /** A download is in progress; [progress] is 0..1 and [message] is user-facing. */
    data class Downloading(val progress: Float, val message: String) : UiState()

    /** A zip extraction is in progress; [progress] is 0..1. */
    data class Extracting(val progress: Float) : UiState()

    /**
     * An incremental-update step is being applied.
     *
     * [index] is the 1-based step number, [total] is the step count,
     * [description] is the user-facing step description, and [progress]
     * is 0..1 for the current step's extraction.
     */
    data class ApplyingUpdate(
        val index: Int,
        val total: Int,
        val description: String,
        val progress: Float
    ) : UiState()

    /** An install/update failed or a launch error occurred; [message] is user-facing. */
    data class Error(val message: String) : UiState()
}
