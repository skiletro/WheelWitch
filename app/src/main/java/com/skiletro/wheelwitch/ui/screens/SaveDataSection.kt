package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.viewmodel.SaveState

@Composable
fun SaveDataSection(
    saveState: SaveState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Text(
        text = "Save Data",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = if (saveState.hasSave) "Save file found" else "No save file found",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var backupFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onBackup,
            enabled = saveState.hasSave,
            shape = buttonShape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onFocusChanged { backupFocused = it.isFocused }
                .focusBorder(backupFocused),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = "Backup",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
        var restoreFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onRestore,
            shape = buttonShape,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .onFocusChanged { restoreFocused = it.isFocused }
                .focusBorder(restoreFocused),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(
                text = "Restore",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
    if (saveState.hasSave) {
        Spacer(modifier = Modifier.height(8.dp))
        var deleteFocused by remember { mutableStateOf(false) }
        TextButton(
            onClick = onDelete,
            modifier = Modifier
                .onFocusChanged { deleteFocused = it.isFocused }
                .focusBorder(deleteFocused, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error)
        ) {
            Text(
                "Delete save data",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
