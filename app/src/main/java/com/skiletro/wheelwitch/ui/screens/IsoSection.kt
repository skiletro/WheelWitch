package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

private val buttonShape = RoundedCornerShape(14.dp)

@Composable
fun IsoSection(
    viewModel: UpdateViewModel,
    onPickIso: () -> Unit
) {
    val isoPath by viewModel.currentIsoPath.collectAsState()
    val fileName = isoPath?.substringAfterLast('/')?.ifBlank { null }

    Text(
        text = "Mario Kart Wii",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (fileName != null) fileName else "Not selected",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onPickIso,
            shape = buttonShape,
            modifier = Modifier.height(48.dp).weight(1f)
        ) {
            Text("Pick ROM", fontWeight = FontWeight.Medium)
        }
        if (fileName != null) {
            OutlinedButton(
                onClick = { viewModel.clearIsoPath() },
                shape = buttonShape,
                modifier = Modifier.height(48.dp).weight(1f)
            ) {
                Text("Clear", fontWeight = FontWeight.Medium)
            }
        }
    }
}
