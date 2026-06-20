package com.skiletro.wheelwitch.ui.launchers

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.skiletro.wheelwitch.data.IsoValidator
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel
import com.skiletro.wheelwitch.viewmodel.SaveDataViewModel
import timber.log.Timber

/** MIME types offered by the ROM file picker. Broadened so the system shows all candidate files. */
val ISO_MIME_TYPES: Array<String> = arrayOf("application/octet-stream", "*/*")

/** Default name suggested by the save-backup document picker. */
const val BACKUP_SUGGESTED_NAME = "rksys.dat"

/**
 * The five SAF launchers used by the home / settings / onboarding flow,
 * bundled into one value so the host composable doesn't need to wire
 * them individually. [launchers] is created via [rememberAppLaunchers].
 */
data class AppLaunchers(
  val storagePermission: ManagedActivityResultLauncher<Intent, androidx.activity.result.ActivityResult>,
  val storageTree: ManagedActivityResultLauncher<android.net.Uri?, android.net.Uri?>,
  val isoFile: ManagedActivityResultLauncher<Array<String>, android.net.Uri?>,
  val backupSave: ManagedActivityResultLauncher<String, android.net.Uri?>,
  val restoreSave: ManagedActivityResultLauncher<Array<String>, android.net.Uri?>,
)

/**
 * Builds and remembers the SAF launchers. The isoFile callback resolves
 * the picked URI to a path, validates the ROM header via [IsoValidator],
 * and on success calls [onIsoPicked] with the resolved path. On failure
 * the picker is silent — the caller surfaces a [android.widget.Toast] via
 * the supplied [onIsoRejected].
 */
@Composable
fun rememberAppLaunchers(
  context: Context,
  packUpdate: PackUpdateViewModel,
  saveData: SaveDataViewModel,
  onIsoPicked: (path: String, isWbfs: Boolean) -> Unit,
  onIsoRejected: () -> Unit,
  onStorageUriPicked: () -> Unit,
): AppLaunchers {
  val storagePermission =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
      // Permission state is re-read on next composition via Environment.isExternalStorageManager()
    }

  val storageTree =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri
      ->
      if (uri != null) {
        try {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
          )
        } catch (e: SecurityException) {
          Timber.tag("AppLaunchers").w(e, "Failed to take persistable URI permission for %s", uri)
          return@rememberLauncherForActivityResult
        }
        packUpdate.setStorageUri(uri)
        onStorageUriPicked()
      }
    }

  val isoFile =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      val path =
        com.skiletro.wheelwitch.data.PackStorage.resolveContentUriToPath(uri)
          ?: return@rememberLauncherForActivityResult run { onIsoRejected() }
      val result = IsoValidator.validate(context, uri)
      if (result is com.skiletro.wheelwitch.data.IsoValidationResult.Valid) {
        val isWbfs =
          result.gameInfo.format == com.skiletro.wheelwitch.data.GameTypeParser.GameFormat.Wbfs
        onIsoPicked(path, isWbfs)
      } else {
        onIsoRejected()
      }
    }

  val backupSave =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("application/octet-stream")) {
        uri ->
      if (uri != null) saveData.backupSave(uri)
    }

  val restoreSave =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) saveData.restoreSave(uri)
    }

  return remember(storagePermission, storageTree, isoFile, backupSave, restoreSave) {
    AppLaunchers(storagePermission, storageTree, isoFile, backupSave, restoreSave)
  }
}

/** Returns true if the app currently holds [Environment.isExternalStorageManager]. */
fun isExternalStorageManager(): Boolean = Environment.isExternalStorageManager()

/** Builds the [Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION] intent for the storage permission request. */
fun buildStoragePermissionIntent(): Intent =
  Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
