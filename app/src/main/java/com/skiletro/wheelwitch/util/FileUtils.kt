package com.skiletro.wheelwitch.util

import java.io.File

/**
 * Returns the total size in bytes of all regular files under [dir].
 * Returns 0 if the directory does not exist.
 */
fun cacheSize(dir: File): Long {
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}
