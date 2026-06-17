package com.skiletro.wheelwitch.ui.components

import java.io.File

/**
 * Formats [bytes] as a human-readable size string.
 *
 * - `< 1 KB` -> "N B"
 * - `< 1 MB` -> "N KB"
 * - else -> "M.NN MB"
 */
fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}

/**
 * Returns the total size in bytes of all regular files under [dir].
 * Returns 0 if the directory does not exist.
 */
fun cacheSize(dir: File): Long {
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
