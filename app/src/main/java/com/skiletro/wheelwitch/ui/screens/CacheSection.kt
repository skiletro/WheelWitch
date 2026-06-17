package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun CacheSection() {
    val context = LocalContext.current
    val cacheDir = remember { File(context.cacheDir, "rewind_pack_downloads") }
    var sizeBytes by remember { mutableStateOf(cacheSize(cacheDir)) }

    Text(
        text = "Cache",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Temporary download files",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatSize(sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(
            onClick = {
                cacheDir.deleteRecursively()
                sizeBytes = 0
            },
            enabled = sizeBytes > 0,
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Clear")
        }
    }
}

fun cacheSize(dir: File): Long {
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}
