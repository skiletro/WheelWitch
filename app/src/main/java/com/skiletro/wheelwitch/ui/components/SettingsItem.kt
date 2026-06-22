package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme

/** Section header used in the settings list, e.g. "Save Data", "Appearance". */
@Composable
fun SettingsCategoryHeader(
  title: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleSmall,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.primary,
    modifier = modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
  )
}

/**
 * Single settings row with a leading icon, two-line title + summary,
 * and an optional trailing slot (button, switch, dropdown, etc.). All
 * text overflows to ellipsis; summary is clamped to two lines.
 */
@Composable
fun SettingsItem(
  icon: ImageVector,
  title: String,
  summary: String? = null,
  iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
  summaryColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  trailing: @Composable (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = iconTint,
      modifier = Modifier.size(24.dp),
    )
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        color = titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (summary != null) {
        Text(
          text = summary,
          style = MaterialTheme.typography.bodySmall,
          color = summaryColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    if (trailing != null) {
      Spacer(modifier = Modifier.width(12.dp))
      trailing()
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun SettingsItemPreview() {
    WheelWitchPreviewTheme {
        SettingsItem(
            icon = ImageVector.vectorResource(R.drawable.ic_info),
            title = "Wheel Witch",
            summary = "Retro Rewind companion app for Android",
            trailing = { androidx.compose.material3.TextButton(onClick = {}) { androidx.compose.material3.Text("Open") } },
        )
    }
}
