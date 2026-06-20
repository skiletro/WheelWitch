package com.skiletro.wheelwitch.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.ui.components.buttonShape
import com.skiletro.wheelwitch.ui.components.sectionShape

/** Card frame that hosts each onboarding step's title, body, and content. */
@Composable
fun StepCard(
  title: String,
  titleColor: Color = MaterialTheme.colorScheme.onSurface,
  titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
  body: String? = null,
  content: @Composable ColumnScope.() -> Unit = {},
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = sectionShape,
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    Column(
      modifier = Modifier.padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = titleStyle,
        fontWeight = FontWeight.Bold,
        color = titleColor,
      )
      if (body != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = body,
          style = MaterialTheme.typography.bodyMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      content()
    }
  }
}

@Composable
fun StepPrimaryButton(text: String, onClick: () -> Unit) {
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = onClick,
    shape = buttonShape,
    modifier = Modifier.fillMaxWidth().height(56.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = MaterialTheme.colorScheme.primary,
      contentColor = MaterialTheme.colorScheme.onPrimary,
    ),
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

@Composable
fun StepSecondaryActions(
  secondaryText: String,
  onSecondary: () -> Unit,
  primaryText: String,
  onPrimary: () -> Unit,
) {
  Spacer(modifier = Modifier.height(16.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedButton(
      onClick = onSecondary,
      shape = buttonShape,
      modifier = Modifier.weight(1f).height(48.dp),
    ) {
      Text(
        text = secondaryText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
    Button(
      onClick = onPrimary,
      shape = buttonShape,
      modifier = Modifier.weight(1f).height(48.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
    ) {
      Text(
        text = primaryText,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}

/** Step progress dots rendered at the bottom of the onboarding flow. */
@Composable
fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
  Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    for (i in 0 until total) {
      val isCompleted = i <= current
      val isCurrent = i == current
      val size by
        animateDpAsState(
          targetValue = if (isCurrent) 10.dp else 8.dp,
          animationSpec = tween(300),
          label = "step_dot_size_$i",
        )
      val color by
        animateColorAsState(
          targetValue =
            if (isCompleted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
          animationSpec = tween(300),
          label = "step_dot_color_$i",
        )
      Box(modifier = Modifier.size(size).clip(CircleShape).background(color))
    }
  }
}
