package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dontsaybojio.rollingnumbers.RollingNumbers
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.RANK_NAMES
import com.skiletro.wheelwitch.model.RANK_THRESH
import com.skiletro.wheelwitch.model.ScoreResult
import com.skiletro.wheelwitch.model.rankFromScore

private fun rankIconRes(rank: Int): Int? = when (rank) {
  1 -> R.drawable.ic_rank_e
  2 -> R.drawable.ic_rank_d
  3 -> R.drawable.ic_rank_c
  4 -> R.drawable.ic_rank_b
  5 -> R.drawable.ic_rank_a
  6 -> R.drawable.ic_rank_1star
  7 -> R.drawable.ic_rank_2star
  8 -> R.drawable.ic_rank_3star
  9 -> R.drawable.ic_rank_crown
  else -> null
}

@Composable
fun RankBadge(result: ScoreResult?) {
  if (result == null) return

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = Modifier.widthIn(min = 70.dp),
  ) {
    if (!result.meetsRaceReq) {
      LockedBadge(result)
    } else {
      PopulatedBadge(result)
    }
  }
}

@Composable
private fun PopulatedBadge(result: ScoreResult) {
  val iconRes = remember(result.rank) { rankIconRes(result.rank) }
  val image = iconRes?.let { painterResource(it) }
  val isMaxRank = result.rank >= 9
  val nextRank = if (!isMaxRank) rankFromScore(result.score) + 1 else null
  val nextIconRes = remember(nextRank) { nextRank?.let { rankIconRes(it) } }
  val nextImage = nextIconRes?.let { painterResource(it) }

  if (image != null) {
    androidx.compose.foundation.Image(
      painter = image,
      contentDescription = RANK_NAMES.getOrElse(result.rank - 1) { "" },
      modifier = Modifier.size(width = 40.dp, height = 48.dp),
    )
  }

  Spacer(Modifier.height(1.dp))

  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
      text = stringResource(R.string.rank_score_label),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.width(3.dp))
    RollingNumbers(
      text = result.score.toInt().toString(),
      textStyle = MaterialTheme.typography.titleSmall.copy(
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
      ),
    )
  }

  Spacer(Modifier.height(1.dp))

  if (!isMaxRank) {
    val currentFloor = RANK_THRESH.getOrNull(result.rank - 1) ?: return
    val nextThresh = RANK_THRESH.getOrNull(result.rank) ?: return
    val range = nextThresh - currentFloor
    val progress = if (range > 0.0) ((result.score - currentFloor) / range).coerceIn(0.0, 1.0).toFloat() else 0f

    LinearProgressIndicator(
      progress = { progress },
      modifier = Modifier
        .width(50.dp)
        .height(4.dp),
      color = MaterialTheme.colorScheme.primary,
      trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )

    Spacer(Modifier.height(1.dp))

    val pointsNeeded = (nextThresh - result.score).toInt()
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "$pointsNeeded pts to ",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      if (nextImage != null) {
        Image(
          painter = nextImage,
          contentDescription = null,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

@Composable
private fun LockedBadge(result: ScoreResult) {
  val iconRes = remember(result.rank) { rankIconRes(result.rank) }
  val image = iconRes?.let { painterResource(it) }

  if (image != null) {
    androidx.compose.foundation.Image(
      painter = image,
      contentDescription = null,
      modifier = Modifier
        .size(width = 40.dp, height = 48.dp)
        .alpha(0.3f),
    )
  }

  Spacer(Modifier.height(1.dp))

  Text(
    text = stringResource(R.string.rank_locked),
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    fontWeight = FontWeight.Bold,
  )

  Text(
    text = stringResource(R.string.rank_races_format, result.totalVs),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    textAlign = TextAlign.Center,
  )
}
