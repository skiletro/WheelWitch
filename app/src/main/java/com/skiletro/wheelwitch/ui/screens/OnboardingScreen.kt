package com.skiletro.wheelwitch.ui.screens

import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.data.GameTypeParser
import com.skiletro.wheelwitch.ui.theme.WheelWitchPreviewTheme
import com.skiletro.wheelwitch.util.launcher.DolphinLauncher
import com.skiletro.wheelwitch.ui.theme.buttonShape
import com.skiletro.wheelwitch.ui.theme.sectionShape
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Duration of the onboarding fade-in/out crossfade. */
private const val ONBOARDING_TRANSITION_MS = 300

/**
 * Onboarding wizard. The flow is:
 * `Welcome → Beta → Storage → Rom → Complete`.
 *
 * - **Storage**: fires the SAF `OpenDocumentTree` picker with
 *   `EXTRA_INITIAL_URI` deep-linked at the Dolphin user folder
 *   (`primary:Android/data/org.dolphinemu.dolphinemu/files`). The
 *   pick is validated by [DolphinTree.validate] and persisted via
 *   [DolphinTree.persist].
 * - **Rom**: fires the SAF `OpenDocument` picker filtered to ISO/RVZ/
 *   WBFS. The picked file is validated by [GameTypeParser.checkValidity]
 *   and copied into the SAF tree by [DolphinTree.copyRomFromSource]
 *   (renamed to `<GAMEID>.<ext>` uppercase).
 *
 * The state machine is in-memory; if the user backgrounds the app
 * mid-flow, they restart at the welcome step on return. The
 * [com.skiletro.wheelwitch.util.prefs.PrefsKeys.ONBOARDING_COMPLETED_KEY]
 * gate is only set in the `Complete` step.
 */
@Composable
fun OnboardingScreen(
  onComplete: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var step by remember { mutableStateOf(OnboardingStep.Welcome) }
  var storageError by remember { mutableStateOf<String?>(null) }
  var romError by remember { mutableStateOf<String?>(null) }
  var isRomLoading by remember { mutableStateOf(false) }
  var romStage by remember { mutableStateOf<String?>(null) }

  // Resolve every error string up here so the activity-result
  // callbacks (which run outside the composition) can use them
  // without calling getString on LocalContext.current — lint
  // rejects that and it's also fragile across recomposition.
  val storageReadFailed = stringResource(R.string.onboarding_storage_read_failed)
  val storageWrongFolder = stringResource(R.string.onboarding_storage_wrong_folder)
  val storageRequired = stringResource(R.string.onboarding_storage_required)
  val isoInvalid = stringResource(R.string.onboarding_iso_invalid)
  val isoCopyFailed = stringResource(R.string.onboarding_iso_copy_failed)
  val romVerifying = stringResource(R.string.onboarding_rom_verifying)
  val romCopying = stringResource(R.string.onboarding_rom_copying)

  // The tree picker. We deep-link to the Dolphin user folder so the
  // user lands at the right place immediately. The picked URI must
  // match the expected tree id (validated by DolphinTree.validate).
  val treeLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocumentTree()) { uri ->
      if (uri == null) {
        // User cancelled. Stay on the Storage step, no error.
        Timber.tag("Onboarding").i("Storage picker cancelled")
        return@rememberLauncherForActivityResult
      }
      Timber.tag("Onboarding")
        .i("Storage picked: authority=%s", uri.authority)
      val validation = DolphinTree.validate(uri)
      validation
        .onSuccess {
          try {
            val tree = DolphinTree(context, uri)
            tree.persistUriPermission()
            DolphinTree.persist(context, tree)
            // Touch the lazy WheelWitch dir so the bootstrap subdirs
            // exist before we move on to the ROM step. If this throws,
            // the user gets a re-promptable error.
            tree.wheelWitchDir // ensure lazy creation succeeds
            Timber.tag("Onboarding")
              .i("Onboarding storage step complete: tree persisted at %s", tree.wheelWitchDir.uri)
            storageError = null
            step = OnboardingStep.Rom
          } catch (e: Exception) {
            Timber.tag("Onboarding").e(e, "Storage bootstrap failed")
            storageError = e.message ?: storageReadFailed
          }
        }
        .onFailure { e ->
          Timber.tag("Onboarding").w(e, "Picked tree failed validation")
          storageError = e.message ?: storageWrongFolder
        }
    }

  // The ROM picker. OpenDocument returns a content URI; we read
  // enough bytes to validate, then copy via DolphinTree (which
  // re-opens the URI for streaming). application/octet-stream is
  // the closest SAF mime filter — extension-based discrimination
  // happens in GameTypeParser.
  val romLauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
      if (uri == null) return@rememberLauncherForActivityResult
      scope.launch {
        isRomLoading = true
        try {
          romStage = romVerifying
          val tree = DolphinTree.fromPersisted(context)
          if (tree == null) {
            romError = storageRequired
            return@launch
          }
          romError = null
          val displayName = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "rom.iso"
          val bytes =
            withContext(Dispatchers.IO) {
              context.contentResolver.openInputStream(uri)?.use { stream ->
                // Read enough bytes for the GameTypeParser checks.
                ByteArray(4096).also { stream.read(it) }
              } ?: throw IOException("Cannot open $uri")
            }
          if (!GameTypeParser.checkValidity(displayName, bytes)) {
            romError = isoInvalid
            return@launch
          }
          val info = GameTypeParser.parseGameInfo(displayName, bytes)
          val gameId = info.gameId ?: "RMCP01"
          val ext = File(displayName).extension.ifEmpty { "iso" }.lowercase()

          romStage = romCopying
          withContext(Dispatchers.IO) { tree.copyRomFromSource(uri, gameId, ext) }
          step = OnboardingStep.Complete
        } catch (e: Exception) {
          Timber.tag("Onboarding").e(e, "ROM copy failed")
          romError = e.message ?: isoCopyFailed
        } finally {
          isRomLoading = false
          romStage = null
        }
      }
    }

  // Pre-warm the deep-link URI for the tree picker. The picker
  // opens at the primary volume's Android/data/org.dolphinemu.dolphinemu/files
  // folder so the user doesn't have to navigate there manually.
  val treeDeepLink =
    remember {
      DocumentsContract.buildTreeDocumentUri(
        "primary",
        "Android/data/${DolphinLauncher.DOLPHIN_PACKAGE}/files",
      )
    }

  BackHandler(enabled = step.previous() != null) {
    storageError = null
    romError = null
    isRomLoading = false
    romStage = null
    step = step.previous() ?: step
  }

  Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    Box(
      modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
      contentAlignment = Alignment.Center,
    ) {
      AnimatedContent(
        targetState = step,
        modifier = Modifier.fillMaxWidth(),
        transitionSpec = {
          (slideInHorizontally(animationSpec = tween(350), initialOffsetX = { it }) +
              fadeIn(animationSpec = tween(250))) togetherWith
            (slideOutHorizontally(animationSpec = tween(350), targetOffsetX = { -it }) +
              fadeOut(animationSpec = tween(200)))
        },
        label = "step",
      ) { currentStep ->
        Column(
          modifier = Modifier.fillMaxWidth(),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          when (currentStep) {
            OnboardingStep.Welcome ->
              WelcomeStep(onNext = { step = OnboardingStep.Beta })
            OnboardingStep.Beta ->
              BetaStep(onNext = { step = OnboardingStep.Storage })
            OnboardingStep.Storage ->
              StorageStep(
                onPick = {
                  storageError = null
                  treeLauncher.launch(treeDeepLink)
                },
                error = storageError,
              )
            OnboardingStep.Rom ->
              RomStep(
                onPick = {
                  romError = null
                  // application/octet-stream is the closest SAF mime
                  // filter; GameTypeParser does extension validation.
                  romLauncher.launch(arrayOf("application/octet-stream", "*/*"))
                },
                error = romError,
                isLoading = isRomLoading,
                stage = romStage,
              )
            OnboardingStep.Complete -> CompleteStep(onDone = onComplete)
          }
        }
      }
    }
    StepDots(
      current = step.ordinal,
      total = OnboardingStep.TOTAL,
      modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
    )
  }
}

/** Onboarding flow. Five steps; TOTAL drives the dot count. */
private enum class OnboardingStep {
  Welcome,
  Beta,
  Storage,
  Rom,
  Complete;

  /** Returns the previous step, or null if this is the first. */
  fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

  companion object {
    val TOTAL: Int = entries.size
  }
}

/** First onboarding step: greets the user. */
@Composable
private fun WelcomeStep(onNext: () -> Unit) {
  StepCard(
    title =
      "${stringResource(R.string.onboarding_welcome_to)}\n${stringResource(R.string.onboarding_app_name)}",
    titleStyle = MaterialTheme.typography.headlineLarge,
    titleColor = MaterialTheme.colorScheme.primary,
    body = stringResource(R.string.onboarding_welcome_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_get_started), onClick = onNext)
  }
}

/** Second onboarding step: surfaces the app's beta status. */
@Composable
private fun BetaStep(onNext: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_beta_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_beta_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_beta_continue), onClick = onNext)
  }
}

/** Third onboarding step: SAF tree picker for the Dolphin user folder. */
@Composable
private fun StorageStep(onPick: () -> Unit, error: String?) {
  StepCard(
    title = stringResource(R.string.onboarding_storage_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_storage_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_select_folder), onClick = onPick)
    if (error != null) {
      Spacer(modifier = Modifier.height(12.dp))
      ErrorBanner(error)
    }
  }
}

/** Fourth onboarding step: SAF document picker for the MKW ROM. */
@Composable
private fun RomStep(onPick: () -> Unit, error: String?, isLoading: Boolean, stage: String?) {
  StepCard(
    title = stringResource(R.string.onboarding_iso_title),
    titleStyle = MaterialTheme.typography.headlineSmall,
    body = stringResource(R.string.onboarding_iso_body),
  ) {
    StepPrimaryButton(
      text = stringResource(R.string.onboarding_select_rom),
      onClick = onPick,
      enabled = !isLoading,
    )
    if (isLoading) {
      Spacer(modifier = Modifier.height(12.dp))
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      if (stage != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = stage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
    if (error != null) {
      Spacer(modifier = Modifier.height(12.dp))
      ErrorBanner(error)
    }
  }
}

/** Final onboarding step: confirms the user is ready to enter the home screen. */
@Composable
private fun CompleteStep(onDone: () -> Unit) {
  StepCard(
    title = stringResource(R.string.onboarding_complete_title),
    titleStyle = MaterialTheme.typography.headlineLarge,
    titleColor = MaterialTheme.colorScheme.primary,
    body = stringResource(R.string.onboarding_complete_body),
  ) {
    StepPrimaryButton(text = stringResource(R.string.onboarding_open_app), onClick = onDone)
  }
}

/** Red-tinted error message shown below the storage or rom step button. */
@Composable
private fun ErrorBanner(message: String) {
  Text(
    text = message,
    color = MaterialTheme.colorScheme.error,
    style = MaterialTheme.typography.bodySmall,
    textAlign = TextAlign.Center,
  )
}

/** Card frame that hosts each onboarding step's title, body, and content. */
@Composable
private fun StepCard(
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
private fun StepPrimaryButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
  Spacer(modifier = Modifier.height(16.dp))
  Button(
    onClick = onClick,
    enabled = enabled,
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

/** Step progress dots rendered at the bottom of the onboarding flow. */
@Composable
private fun StepDots(current: Int, total: Int, modifier: Modifier = Modifier) {
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

/**
 * Best-effort display-name lookup for a SAF document URI. Falls back
 * to `null` so the caller can use a generic name. DocumentFile is
 * the only public API for this — `DocumentsContract.getTreeDocumentId`
 * doesn't return the leaf name.
 */
private fun queryDisplayName(context: android.content.Context, uri: Uri): String? =
  DocumentFile.fromSingleUri(context, uri)?.name

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun OnboardingScreenWelcomePreview() {
    WheelWitchPreviewTheme {
        OnboardingScreen(onComplete = {})
    }
}
