package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.ui.theme.surfaceShape

@Composable
fun LicenseGrid(
  licenses: List<LicenseInfo>,
  isLoading: Boolean,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      licenses.chunked(2).forEach { pair ->
        Row(
          modifier = Modifier.fillMaxWidth().weight(1f),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          LicenseCell(
            license = pair.getOrNull(0),
            modifier = Modifier.weight(1f),
          )
          LicenseCell(
            license = pair.getOrNull(1),
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
    if (isLoading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
  }
}

@Composable
fun LicenseCell(
  license: LicenseInfo?,
  modifier: Modifier = Modifier,
) {
  val exists = license?.exists == true
  val background =
    if (exists) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

  Surface(
    modifier = modifier,
    shape = surfaceShape,
    color = background,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      val populated = license?.takeIf { it.exists }
      if (populated != null) {
        PopulatedCell(license = populated)
      } else {
        EmptyCell()
      }
    }
  }
}

@Composable
fun PopulatedCell(license: LicenseInfo) {
  Row(
    modifier = Modifier.fillMaxSize().padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MiiFace(
      imageBase64 = null,
      miiDataBase64 = license.miiDataBase64,
      modifier = Modifier.size(84.dp),
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = license.miiName ?: stringResource(R.string.save_info_no_name),
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        maxLines = 1,
        fontFamily = CtmkfFontFamily,
        color = MaterialTheme.colorScheme.onSurface,
      )
      license.friendCode?.let { fc ->
        Text(
          text = fc,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(6.dp))
      val vr = license.leaderboardVr ?: 0
      Text(
        text = stringResource(R.string.save_info_vr_format, vr),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Spacer(modifier = Modifier.height(2.dp))
      val wins = license.raceWins ?: 0
      val losses = license.raceLosses ?: 0
      Text(
        text = stringResource(R.string.save_info_race_format, wins, losses),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun EmptyCell() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      text = stringResource(R.string.save_info_empty_slot),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
  }
}

@Composable
fun EmptySaveBody(isLoading: Boolean) {
  Box(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
    contentAlignment = Alignment.Center,
  ) {
    if (isLoading) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text(
          text = stringResource(R.string.save_info_loading),
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    } else {
      Text(
        text = stringResource(R.string.save_info_no_licenses),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}