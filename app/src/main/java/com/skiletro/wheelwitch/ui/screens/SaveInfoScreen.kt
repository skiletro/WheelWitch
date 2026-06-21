package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme

/**
 * Stub Licenses screen. The full save-data license parser (RksysParser)
 * and the underlying backup / restore / delete logic have been ripped
 * out for a planned rewrite. The top-bar icon still routes here so the
 * navigation surface remains, but the content is a placeholder.
 */
@Composable
fun SaveInfoScreen(
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = stringResource(R.string.save_info_title),
            onBack = onClose,
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.licenses_coming_soon),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun SaveInfoScreenPreview() {
    WheelWitchPreviewTheme {
        SaveInfoScreen(onClose = {})
    }
}
