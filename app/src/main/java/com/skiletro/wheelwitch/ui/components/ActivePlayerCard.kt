package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dontsaybojio.rollingnumbers.RollingNumbers
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily

/**
 * Compact Mii avatar + license name + VR display. Sourced from the
 * currently selected save slot ([LicenseInfo]). When [vrMultiplier]
 * is above 1.0, the VR label is suffixed with the multiplier to
 * reflect the active bonus (e.g. "2x" or "1.5x").
 */
@Composable
fun ActivePlayerCard(license: LicenseInfo, cachedLeaderboardVr: Int?, vrMultiplier: Float?) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    MiiFace(
      imageBase64 = license.leaderboardMiiImageBase64,
      miiDataBase64 = license.miiDataBase64,
      modifier = Modifier.size(40.dp),
    )
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(
        text = license.miiName ?: stringResource(R.string.player_default),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        fontFamily = CtmkfFontFamily,
      )
      val vr = license.leaderboardVr ?: cachedLeaderboardVr ?: 0
      val showActive = vrMultiplier != null && vrMultiplier > 1.0f
      val multText =
        if (showActive) {
          if (vrMultiplier == vrMultiplier.toInt().toFloat()) vrMultiplier.toInt().toString()
          else vrMultiplier.toString()
        } else null
      val suffix =
        if (multText != null) stringResource(R.string.home_vr_active_suffix, multText)
        else stringResource(R.string.home_vr_suffix)
      val numberStyle =
        MaterialTheme.typography.labelSmall.copy(
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      Row(verticalAlignment = Alignment.CenterVertically) {
        RollingNumbers(text = vr.toString(), textStyle = numberStyle)
        Text(
          text = " $suffix",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,)
      }
    }
  }
}
