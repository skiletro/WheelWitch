package com.skiletro.wheelwitch.model

data class SemVersion(val major: Int, val minor: Int, val patch: Int, val preRelease: String? = null) :
    Comparable<SemVersion> {

    override fun compareTo(other: SemVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        return 0
    }

    override fun toString(): String {
        return if (preRelease != null) "$major.$minor.$patch-$preRelease" else "$major.$minor.$patch"
    }

    companion object {
        fun parse(text: String): SemVersion? {
            val cleaned = text.trimStart('v', 'V')
            val dashIdx = cleaned.indexOf('-')
            val versionPart = if (dashIdx >= 0) cleaned.substring(0, dashIdx) else cleaned
            val preRelease = if (dashIdx >= 0) cleaned.substring(dashIdx + 1) else null
            val parts = versionPart.split(".")
            if (parts.size < 3) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return SemVersion(major, minor, patch, preRelease)
        }
    }
}
