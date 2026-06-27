package com.skiletro.wheelwitch.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.ChangelogEntry
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme
import com.skiletro.wheelwitch.ui.theme.surfaceShape

private const val Bullet = "\u2022"

/**
 * Renders one [ChangelogEntry] as a card: version + date header row
 * followed by a bulleted list of changes.
 *
 * When [isNew] is true, a small primary-colored "NEW" pill is
 * rendered to the right of the date with a gentle alpha pulse.
 */
@Composable
fun ChangelogCard(entry: ChangelogEntry, isNew: Boolean = false, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = surfaceShape,
    color = MaterialTheme.colorScheme.surfaceVariant
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = entry.version,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary
        )
        if (entry.date.isNotBlank()) {
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = entry.date,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        if (isNew) {
          Spacer(modifier = Modifier.weight(1f))
          NewBadge()
        }
      }
      if (entry.changes.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        for (change in entry.changes) {
          Row(
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
            verticalAlignment = Alignment.Top
          ) {
            Text(
              text = Bullet,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.width(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = change,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }
    }
  }
}

@Composable
private fun NewBadge() {
  val transition = rememberInfiniteTransition(label = "new_badge_pulse")
  val alpha by
    transition.animateFloat(
      initialValue = 0.65f,
      targetValue = 1f,
      animationSpec =
        infiniteRepeatable(
          animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
          repeatMode = RepeatMode.Reverse
        ),
      label = "new_badge_alpha"
    )
  Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primary) {
    Text(
      text = stringResource(R.string.version_history_new),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.alpha(alpha).padding(horizontal = 8.dp, vertical = 2.dp)
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ChangelogCardPreview() {
  WheelWitchPreviewTheme {
    ChangelogCard(
      entry =
        ChangelogEntry(
          version = "6.11.1",
          date = "2026-01-15",
          changes = listOf("Added 8 new tracks", "Improved room join reliability"),
        ),
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ChangelogCardNewPreview() {
  WheelWitchPreviewTheme {
    ChangelogCard(
      entry =
        ChangelogEntry(
          version = "6.11.1",
          date = "2026-01-15",
          changes = listOf("Added 8 new tracks", "Improved room join reliability"),
        ),
      isNew = true,
    )
  }
}
