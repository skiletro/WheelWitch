package com.skiletro.wheelwitch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.ui.screens.SplashScreen
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WheelWitchTheme {
                MainScreen()
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: UpdateViewModel = viewModel()) {
    val context = LocalContext.current

    val storagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.setStorageUri(uri)
        }
    }

    val isoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = resolveContentUriToPath(uri)
            if (path != null) {
                viewModel.setGameIsoPath(path)
            }
        }
    }

    val backupPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.backupSave(uri)
        }
    }

    val restorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreSave(uri)
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) {
        SplashScreen(
            viewModel = viewModel,
            onPickStorage = { storagePicker.launch(null) },
            onPickIso = { isoPicker.launch(arrayOf("application/octet-stream", "*/*")) },
            onBackupSave = { backupPicker.launch("rksys.dat") },
            onRestoreSave = { restorePicker.launch(arrayOf("application/octet-stream", "*/*")) },
            onDeleteSave = { viewModel.deleteSave() }
        )
    }
}

private fun resolveContentUriToPath(uri: Uri): String? {
    val docId = try {
        android.provider.DocumentsContract.getDocumentId(uri)
    } catch (e: Exception) {
        return uri.path
    }
    val parts = docId.split(":")
    if (parts.size < 2) return uri.path
    return when {
        parts[0].equals("primary", ignoreCase = true) ->
            "/storage/emulated/0/${parts[1]}"
        else ->
            "/storage/${parts[0]}/${parts[1]}"
    }
}
