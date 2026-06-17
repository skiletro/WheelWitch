package com.skiletro.wheelwitch.util

import android.net.Uri

fun resolveContentUriToPath(uri: Uri): String? {
    val docId = try {
        android.provider.DocumentsContract.getDocumentId(uri)
    } catch (e: Exception) {
        return uri.path
    }
    val parts = docId.split(":")
    if (parts.size < 2) return uri.path
    return when {
        parts[0].equals("primary", ignoreCase = true) ->
            "/storage/emulated/0/${parts[1]}"
        else ->
            "/storage/${parts[0]}/${parts[1]}"
    }
}
