package com.skiletro.wheelwitch.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme
import com.skiletro.wheelwitch.ui.theme.sectionShape
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel

/**
 * Licenses screen — parses the active region's `rksys.dat` and shows
 * the 4 license slots, with backup / restore / delete actions and
 * (in a multi-region install) a region picker.
 *
 * The data lives in [SaveDataViewModel]; this composable just
 * observes it. The [SaveDataViewModel.refresh] call is triggered by
 * the pack-install flow, so this screen automatically re-reads the
 * save after every install/update without explicit wiring here.
 */
@Composable
fun SaveInfoScreen(
  viewModel: SaveDataViewModel,
  onClose: () -> Unit,
) {
  val saveInfos by viewModel.saveInfos.collectAsState()
  val hasSave by viewModel.hasSave.collectAsState()
  val selectedRegion by viewModel.selectedRegion.collectAsState()
  val selectedSlotIndex by viewModel.selectedSlotIndex.collectAsState()
  val activeLicense by viewModel.activeLicense.collectAsState()
  val isLoading by viewModel.isLoading.collectAsState()
  val error by viewModel.error.collectAsState()
  val cachedLeaderboardVrs by viewModel.cachedLeaderboardVrs.collectAsState()

  val context = LocalContext.current
  var showDeleteConfirm by remember { mutableStateOf(false) }
  var pendingBackup by remember { mutableStateOf(false) }
  var pendingRestore by remember { mutableStateOf(false) }

  BackHandler(onBack = onClose)

  val backupLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
      val region = selectedRegion
      if (uri != null && region != null) {
        viewModel.backupSave(region, uri)
      }
      pendingBackup = false
    }

  val restoreLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
      val region = selectedRegion
      if (uri != null && region != null) {
        viewModel.restoreSave(region, uri)
      }
      pendingRestore = false
    }

  LaunchedEffect(error) {
    val message = error
    if (message != null) {
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
      viewModel.clearError()
    }
  }

  LaunchedEffect(pendingBackup, selectedRegion) {
    val region = selectedRegion
    if (pendingBackup && region != null) {
      val fileName = "rksys-${region.code}-${System.currentTimeMillis()}.dat"
      backupLauncher.launch(fileName)
    }
  }

  LaunchedEffect(pendingRestore) {
    if (pendingRestore) {
      restoreLauncher.launch(arrayOf("application/octet-stream", "*/*"))
    }
  }

  if (showDeleteConfirm) {
    val region = selectedRegion
    if (region != null) {
      val name =
        when (region) {
          SaveManager.Region.PAL -> stringResource(R.string.save_info_region_pal)
          SaveManager.Region.USA -> stringResource(R.string.save_info_region_usa)
          SaveManager.Region.JPN -> stringResource(R.string.save_info_region_jpn)
        }
      AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text(stringResource(R.string.save_info_delete_confirm_title)) },
        text = {
          Text(stringResource(R.string.save_info_delete_confirm_message, name))
        },
        confirmButton = {
          Button(
            onClick = {
              viewModel.deleteSave(region)
              showDeleteConfirm = false
            },
          ) {
            Text(stringResource(R.string.save_info_delete))
          }
        },
        dismissButton = {
          TextButton(onClick = { showDeleteConfirm = false }) {
            Text(stringResource(R.string.action_cancel))
          }
        },
      )
    } else {
      showDeleteConfirm = false
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
  ) {
    ScreenHeader(
      title = stringResource(R.string.save_info_title),
      onBack = onClose,
    )

    val regions = saveInfos.keys.toList()
    if (regions.isEmpty()) {
      EmptySaveBody(
        isLoading = isLoading,
        hasSaveForSelected = (selectedRegion?.let { hasSave[it] }) ?: false,
      )
    } else {
      SaveBody(
        regions = regions,
        selectedRegion = selectedRegion,
        onSelectRegion = viewModel::selectRegion,
        slotCount = 4,
        selectedSlotIndex = selectedSlotIndex,
        onSelectSlot = viewModel::selectSlot,
        activeLicense = activeLicense,
        cachedLeaderboardVrs = cachedLeaderboardVrs,
        regionHasSave = { r -> hasSave[r] ?: false },
        isLoading = isLoading,
        onBackup = { pendingBackup = true },
        onRestore = { pendingRestore = true },
        onDelete = { showDeleteConfirm = true },
      )
    }
  }
}

/** Body when the user has no save files at all (no ROM picked, or all regions empty). */
@Composable
private fun EmptySaveBody(isLoading: Boolean, hasSaveForSelected: Boolean) {
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
        text =
          if (hasSaveForSelected) {
            stringResource(R.string.save_info_no_licenses)
          } else {
            stringResource(R.string.status_save_not_found)
          },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
private fun SaveBody(
  regions: List<SaveManager.Region>,
  selectedRegion: SaveManager.Region?,
  onSelectRegion: (SaveManager.Region) -> Unit,
  slotCount: Int,
  selectedSlotIndex: Int,
  onSelectSlot: (Int) -> Unit,
  activeLicense: LicenseInfo?,
  cachedLeaderboardVrs: Map<Int, Int>,
  regionHasSave: (SaveManager.Region) -> Boolean,
  isLoading: Boolean,
  onBackup: () -> Unit,
  onRestore: () -> Unit,
  onDelete: () -> Unit,
) {
  val region = selectedRegion ?: regions.first()
  val info = activeLicense

  Column(
    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (regions.size > 1) {
      RegionPicker(
        regions = regions,
        selected = region,
        onSelect = onSelectRegion,
        hasSave = regionHasSave,
      )
    }

    SlotPicker(
      slotCount = slotCount,
      selected = selectedSlotIndex,
      onSelect = onSelectSlot,
      cachedLeaderboardVrs = cachedLeaderboardVrs,
    )

    AnimatedContent(
      targetState = info to isLoading,
      transitionSpec = { fadeIn() togetherWith fadeOut() },
      label = "license_card",
    ) { (license, loading) ->
      if (loading) {
        Box(
          modifier = Modifier.fillMaxWidth().height(180.dp),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }
      } else {
        LicenseCard(license = license)
      }
    }

    Spacer(Modifier.height(8.dp))

    ActionRow(
      onBackup = onBackup,
      onRestore = onRestore,
      onDelete = onDelete,
      hasSave = regionHasSave(region),
    )
  }
}

@Composable
private fun RegionPicker(
  regions: List<SaveManager.Region>,
  selected: SaveManager.Region,
  onSelect: (SaveManager.Region) -> Unit,
  hasSave: (SaveManager.Region) -> Boolean,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.save_info_region_picker),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      regions.forEach { region ->
        val labelRes =
          when (region) {
            SaveManager.Region.PAL -> R.string.save_info_region_pal
            SaveManager.Region.USA -> R.string.save_info_region_usa
            SaveManager.Region.JPN -> R.string.save_info_region_jpn
          }
        val isSelected = region == selected
        FilterChip(
          selected = isSelected,
          onClick = { onSelect(region) },
          label = { Text(stringResource(labelRes)) },
          leadingIcon = {
            if (hasSave(region)) {
              Box(
                modifier =
                  Modifier.size(8.dp).clip(CircleShape).background(
                      MaterialTheme.colorScheme.primary
                  )
              )
            } else {
              Box(
                modifier =
                  Modifier.size(8.dp).clip(CircleShape).background(
                      MaterialTheme.colorScheme.surfaceVariant
                  )
              )
            }
          },
        )
      }
    }
  }
}

@Composable
private fun SlotPicker(
  slotCount: Int,
  selected: Int,
  onSelect: (Int) -> Unit,
  cachedLeaderboardVrs: Map<Int, Int>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.save_info_slot_picker),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(slotCount) { index ->
        val isSelected = index == selected
        val cachedVr = cachedLeaderboardVrs[index]
        val label =
          if (cachedVr != null) {
            stringResource(R.string.save_info_slot_format, index + 1) + " · VR $cachedVr"
          } else {
            stringResource(R.string.save_info_slot_format, index + 1)
          }
        FilterChip(
          selected = isSelected,
          onClick = { onSelect(index) },
          label = { Text(label) },
          colors =
            FilterChipDefaults.filterChipColors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        )
      }
    }
  }
}

@Composable
private fun LicenseCard(license: LicenseInfo?) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = sectionShape,
    color = MaterialTheme.colorScheme.surfaceVariant,
  ) {
    if (license == null || !license.exists) {
      Box(
        modifier = Modifier.fillMaxWidth().height(180.dp).padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.save_info_empty_slot),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      return@Surface
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      MiiFace(
        imageBase64 = license.leaderboardMiiImageBase64,
        miiDataBase64 = license.miiDataBase64,
        modifier = Modifier.size(96.dp),
      )
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = license.miiName ?: stringResource(R.string.save_info_no_name),
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        if (license.friendCode != null) {
          Text(
            text = stringResource(R.string.save_info_friend_code_format, license.friendCode),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        val vr = license.leaderboardVr ?: license.vr
        if (vr != null) {
          val text =
            if (license.leaderboardVr != null && license.vr != null &&
                license.leaderboardVr != license.vr
            ) {
              stringResource(
                R.string.save_info_vr_leaderboard_format,
                license.vr,
                license.leaderboardVr,
              )
            } else {
              stringResource(R.string.save_info_vr_format, vr)
            }
          Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        val wins = license.raceWins
        val losses = license.raceLosses
        if (wins != null && losses != null) {
          Text(
            text = stringResource(R.string.save_info_race_format, wins, losses),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun ActionRow(
  onBackup: () -> Unit,
  onRestore: () -> Unit,
  onDelete: () -> Unit,
  hasSave: Boolean,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    FilledTonalButton(
      onClick = onBackup,
      enabled = hasSave,
      modifier = Modifier.weight(1f).height(48.dp),
    ) {
      Text(stringResource(R.string.save_info_backup))
    }
    FilledTonalButton(
      onClick = onRestore,
      modifier = Modifier.weight(1f).height(48.dp),
    ) {
      Text(stringResource(R.string.save_info_restore))
    }
    Button(
      onClick = onDelete,
      enabled = hasSave,
      colors =
        ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.errorContainer,
          contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
      modifier = Modifier.weight(1f).height(48.dp),
    ) {
      Text(stringResource(R.string.save_info_delete))
    }
  }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
private fun LicenseCardPreview() {
  WheelWitchPreviewTheme { LicenseCard(license = null) }
}
