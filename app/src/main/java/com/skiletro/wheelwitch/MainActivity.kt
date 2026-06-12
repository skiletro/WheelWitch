package com.skiletro.wheelwitch

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.service.DolphinLauncher
import com.skiletro.wheelwitch.ui.screens.SplashScreen
import com.skiletro.wheelwitch.ui.theme.WheelWitchTheme
import com.skiletro.wheelwitch.viewmodel.UpdateViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            DolphinLauncher.setGameIsoUri(context, uri.toString())
            viewModel.clearError()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        SplashScreen(
            viewModel = viewModel,
            onPickStorage = { storagePicker.launch(null) },
            onPickIso = { isoPicker.launch(arrayOf("*/*")) }
        )
    }
}
