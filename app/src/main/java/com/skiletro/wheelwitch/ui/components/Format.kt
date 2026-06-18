package com.skiletro.wheelwitch.ui.components

/**
 * Formats [bytes] as a human-readable size string.
 *
 * - `< 1 KB` -> "N B"
 * - `< 1 MB` -> "N KB" (integer division; rolls to MB at >= 1 MiB)
 * - else -> "M.NN MB"
 */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}
