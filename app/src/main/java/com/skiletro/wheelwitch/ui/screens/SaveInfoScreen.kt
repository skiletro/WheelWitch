package com.skiletro.wheelwitch.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.components.FocusableSurface
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme
import com.skiletro.wheelwitch.ui.theme.surfaceShape
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/**
 * Licenses screen: 2x2 grid of the 4 license slots for the
 * currently selected region. Tapping a slot selects it (the active
 * license used elsewhere in the app). Region selection lives in
 * the Settings Save Data section; this screen is a pure viewer.
 *
 * The data lives in [SaveDataViewModel]; this composable just
 * observes it. [SaveDataViewModel.refresh] re-parses the save and
 * re-merges the leaderboard for the selected region's 4 slots
 * whenever the pack state transitions to Ready.
 *
 * Auto-refresh: a `DisposableEffect` listens for `Lifecycle.Event.ON_RESUME`
 * and triggers a refresh. The observer receives the current state on
 * subscription, so the first composition also refreshes (matching the
 * pattern used by [OnboardingScreen] for the Dolphin install check and
 * by [HomeScreen] for room refreshes).
 */
@Composable
fun SaveInfoScreen(viewModel: SaveDataViewModel, onClose: () -> Unit) {
  val selectedRegion by viewModel.selectedRegion.collectAsState()
  val selectedSlotIndex by viewModel.selectedSlotIndex.collectAsState()
  val mergedLicenses by viewModel.mergedLicenses.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val error by viewModel.error.collectAsState()

  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  BackHandler(onBack = onClose)

  LaunchedEffect(error) {
    val message = error
    if (message != null) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
      viewModel.clearError()
    }
  }

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }

  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    ScreenHeader(
      title = stringResource(R.string.save_info_title),
      onBack = onClose,
      onRefresh = viewModel::refresh,
    )

    val licenses = selectedRegion?.let { mergedLicenses[it] }
    if (selectedRegion == null || licenses == null) {
      EmptySaveBody(isLoading = isLoading)
    } else {
      LicenseGrid(
        licenses = licenses,
        selectedSlotIndex = selectedSlotIndex,
        onSelect = viewModel::selectSlot,
        isLoading = isLoading,
      )
    }
  }
}

/** Body when the user has no ROMs / no save at all. */
@Composable
private fun EmptySaveBody(isLoading: Boolean) {
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

/** 2x2 grid of license cards with no scroll, sized to fill the screen. */
@Composable
private fun LicenseGrid(
  licenses: List<LicenseInfo>,
  selectedSlotIndex: Int,
  onSelect: (Int) -> Unit,
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
            isSelected = pair.getOrNull(0)?.let { it.slotIndex == selectedSlotIndex } == true,
            onSelect = { pair.getOrNull(0)?.let { onSelect(it.slotIndex) } },
            modifier = Modifier.weight(1f),
          )
          LicenseCell(
            license = pair.getOrNull(1),
            isSelected = pair.getOrNull(1)?.let { it.slotIndex == selectedSlotIndex } == true,
            onSelect = { pair.getOrNull(1)?.let { onSelect(it.slotIndex) } },
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
private fun LicenseCell(
  license: LicenseInfo?,
  isSelected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val exists = license?.exists == true
  val background =
    if (exists) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

  FocusableSurface(
    modifier = modifier,
    onClick = onSelect,
    enabled = exists,
    selected = isSelected,
    shape = surfaceShape,
    color = background,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (exists && license != null) {
        PopulatedCell(license = license)
        if (isSelected) {
          SelectedBadge(modifier = Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
      } else {
        EmptyCell()
      }
    }
  }
}

@Composable
private fun PopulatedCell(license: LicenseInfo) {
  Row(
    modifier = Modifier.fillMaxSize().padding(14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    MiiFace(
      imageBase64 = license.leaderboardMiiImageBase64,
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
      val vr = license.leaderboardVr ?: license.vr
      if (vr != null) {
        val text =
          if (license.leaderboardVr != null && license.vr != null && license.leaderboardVr != license.vr
          ) {
            stringResource(R.string.save_info_vr_leaderboard_format, license.vr, license.leaderboardVr)
          } else {
            stringResource(R.string.save_info_vr_format, vr)
          }
        Text(
          text = text,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
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
private fun EmptyCell() {
  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(
      text = stringResource(R.string.save_info_empty_slot),
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    )
  }
}

@Composable
private fun SelectedBadge(modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.ic_star),
      contentDescription = stringResource(R.string.save_info_selected),
      tint = MaterialTheme.colorScheme.onPrimary,
      modifier = Modifier.size(16.dp),
    )
  }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 480)
@Composable
private fun SaveInfoScreenPreview() {
  WheelWitchPreviewTheme {
    val licenses =
      listOf(
        LicenseInfo(
          slotIndex = 0,
          exists = true,
          miiName = "Player One",
          friendCode = "1234-5678-9012",
          vr = 5000,
          raceWins = 100,
          raceLosses = 50,
        ),
        LicenseInfo(slotIndex = 1, exists = false),
        LicenseInfo(
          slotIndex = 2,
          exists = true,
          miiName = "Player Three",
          friendCode = "9876-5432-1098",
          vr = 2500,
          raceWins = 30,
          raceLosses = 70,
        ),
        LicenseInfo(slotIndex = 3, exists = false),
      )
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
      ScreenHeader(title = stringResource(R.string.save_info_title), onBack = {})
      LicenseGrid(licenses = licenses, selectedSlotIndex = 0, onSelect = {}, isLoading = false)
    }
  }
}
