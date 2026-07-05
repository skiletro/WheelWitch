package com.skiletro.wheelwitch.util.display

import android.app.Presentation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.viewmodel.HealthState
import com.skiletro.wheelwitch.viewmodel.UiState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

private const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
private const val DIM_GRAY = 0xFF888888.toInt()
private const val DOT_OFF = 0xFF222222.toInt()
private const val DOT_ON = 0xFF4CAF50.toInt()
private const val DOT_RED = 0xFFFF4444.toInt()
private const val SEGMENT_COUNT = 8

class SecondScreenPresentation(
  context: Context,
  display: Display,
  private val activeLicenseFlow: StateFlow<LicenseInfo?>,
  private val packStatusFlow: StateFlow<UiState>,
  private val healthStateFlow: StateFlow<HealthState>,
) : Presentation(context, display) {

  private val handler = Handler(Looper.getMainLooper())
  private lateinit var timeView: TextView
  private lateinit var batteryText: TextView
  private lateinit var batteryBar: LinearLayout
  private lateinit var dolphinDot: View
  private lateinit var healthDot: View
  private lateinit var licenseView: TextView
  private lateinit var packView: TextView

  private val updateRunnable = object : Runnable {
    override fun run() {
      updateTime()
      updateBattery()
      updateDolphinDot()
      updateHealthDot()
      updateLicense()
      updatePackInfo()
      handler.postDelayed(this, CLOCK_INTERVAL_MS)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window?.apply {
      setBackgroundDrawable(ColorDrawable(Color.BLACK))
      attributes = attributes.apply {
        screenBrightness = 0.05f
      }
      addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
      addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    }

    val density = resources.displayMetrics.density
    val dp4 = (4 * density).toInt()
    val dp8 = (8 * density).toInt()
    val dp12 = (12 * density).toInt()
    val dp16 = (16 * density).toInt()

    val root =
      LinearLayout(getContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp16, dp12, dp16, 0)
        setBackgroundColor(Color.BLACK)
      }

    // ── Row 1: time · battery bar + % · dots ──────────────────────
    val row1 =
      LinearLayout(getContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          )
        gravity = Gravity.CENTER_VERTICAL
      }

    timeView =
      TextView(getContext()).apply {
        text = ""
        setTextColor(DIM_GRAY)
        textSize = 18f
        layoutParams =
          LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
      }
    row1.addView(timeView)

    // battery bar + text
    batteryBar =
      LinearLayout(getContext()).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
      }
    // add initial segments
    rebuildBatteryBar(0)
    row1.addView(batteryBar)

    batteryText =
      TextView(getContext()).apply {
        text = ""
        setTextColor(DIM_GRAY)
        textSize = 12f
      }
    row1.addView(batteryText)

    // spacer between battery and dots
    row1.addView(
      View(getContext()).apply {
        layoutParams =
          LinearLayout.LayoutParams(dp12, LinearLayout.LayoutParams.MATCH_PARENT)
      }
    )

    dolphinDot =
      View(getContext()).apply {
        val size = (8 * density).toInt()
        layoutParams = LinearLayout.LayoutParams(size, size)
        setBackgroundColor(DOT_OFF)
      }
    row1.addView(dolphinDot)

    healthDot =
      View(getContext()).apply {
        val size = (8 * density).toInt()
        layoutParams =
          LinearLayout.LayoutParams(size, size).apply { leftMargin = dp8 }
        setBackgroundColor(DOT_OFF)
      }
    row1.addView(healthDot)

    root.addView(row1)

    // ── Row 2: license info ───────────────────────────────────────
    licenseView =
      TextView(getContext()).apply {
        text = ""
        setTextColor(DIM_GRAY)
        textSize = 12f
        visibility = View.GONE
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          ).apply { topMargin = dp4 }
      }
    root.addView(licenseView)

    // ── Row 3: pack version + health text ──────────────────────────
    packView =
      TextView(getContext()).apply {
        text = ""
        setTextColor(DIM_GRAY)
        textSize = 12f
        visibility = View.GONE
        layoutParams =
          LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
          )
      }
    root.addView(packView)

    setContentView(root)
    handler.post(updateRunnable)
  }

  override fun onStop() {
    super.onStop()
    handler.removeCallbacks(updateRunnable)
  }

  // ── Update methods ────────────────────────────────────────────────

  private fun updateTime() {
    timeView.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
  }

  private fun updateBattery() {
    try {
      val bm = getContext().getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
      val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
      val intent = getContext().registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
      val charging =
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

      rebuildBatteryBar(level)
      batteryText.text = if (charging) "$level% ⚡" else "$level%"
    } catch (_: Exception) {
      // ignore
    }
  }

  private fun updateDolphinDot() {
    val running = isDolphinProcessRunning()
    dolphinDot.setBackgroundColor(if (running) DOT_ON else DOT_OFF)
  }

  private fun updateHealthDot() {
    val state = healthStateFlow.value
    val dotColor =
      when (state) {
        is HealthState.Success -> DOT_ON
        is HealthState.Error -> DOT_RED
        else -> DOT_OFF
      }
    healthDot.setBackgroundColor(dotColor)
  }

  private fun updateLicense() {
    val running = isDolphinProcessRunning()
    val license = activeLicenseFlow.value
    if (running && license != null && license.exists) {
      licenseView.text = buildLicenseText(license)
      licenseView.visibility = View.VISIBLE
    } else {
      licenseView.visibility = View.GONE
    }
  }

  private fun updatePackInfo() {
    val running = isDolphinProcessRunning()
    if (running) {
      val version = extractPackVersion()
      val healthOk = isServerHealthy()
      packView.text =
        buildString {
          if (version != null) append("$version · ")
          append("Retro WFC ")
          append(if (healthOk) "●" else "○")
        }
      packView.visibility = View.VISIBLE
    } else {
      packView.visibility = View.GONE
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────

  private fun buildLicenseText(license: LicenseInfo): String =
    buildString {
      append(license.miiName ?: "")
      append(" · VR ${license.vr ?: 0}")
      val wins = license.raceWins
      val losses = license.raceLosses
      if (wins != null && losses != null) {
        append(" · ${wins}W ${losses}L")
      }
      val fc = license.friendCode
      if (fc != null) append(" · $fc")
    }

  private fun extractPackVersion(): String? {
    val uiState = packStatusFlow.value
    if (uiState !is UiState.Ready) return null
    return when (val s = uiState.status) {
      is PackStatus.UpToDate -> s.currentVersion.toString()
      is PackStatus.UpdateAvailable -> s.currentVersion.toString()
      is PackStatus.CheckFailed -> s.installedVersion?.toString()
      else -> null
    }
  }

  private fun isServerHealthy(): Boolean {
    val state = healthStateFlow.value
    return state is HealthState.Success && state.health.status == "ok"
  }

  private fun rebuildBatteryBar(level: Int) {
    batteryBar.removeAllViews()
    val density = resources.displayMetrics.density
    val segW = (5 * density).toInt()
    val segH = (12 * density).toInt()
    val gap = (2 * density).toInt()
    val filled = ((level / 100f) * SEGMENT_COUNT).toInt().coerceIn(0, SEGMENT_COUNT)

    repeat(SEGMENT_COUNT) { i ->
      batteryBar.addView(
        View(getContext()).apply {
          layoutParams =
            LinearLayout.LayoutParams(segW, segH).apply {
              rightMargin = gap
            }
          setBackgroundColor(if (i < filled) DIM_GRAY else DOT_OFF)
        }
      )
    }
    // nub
    batteryBar.addView(
      View(getContext()).apply {
        layoutParams =
          LinearLayout.LayoutParams((2 * density).toInt(), (7 * density).toInt())
        setBackgroundColor(DIM_GRAY)
      }
    )
  }

  private fun isDolphinProcessRunning(): Boolean {
    return try {
      File("/proc").listFiles()?.any { file ->
        if (!file.isDirectory) return@any false
        file.name.toIntOrNull() ?: return@any false
        try {
          DOLPHIN_PACKAGE in File(file, "cmdline").readText()
        } catch (_: Exception) {
          false
        }
      } ?: false
    } catch (_: Exception) {
      false
    }
  }

  companion object {
    private const val CLOCK_INTERVAL_MS = 30_000L
  }
}
