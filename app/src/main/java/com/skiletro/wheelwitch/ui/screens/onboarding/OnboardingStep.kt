package com.skiletro.wheelwitch.ui.screens.onboarding

/** Ordered list of onboarding pages. [ordinal] doubles as the [StepDots] index. */
internal enum class OnboardingStep {
  Welcome,
  Beta,
  Permission,
  Dolphin,
  Storage,
  Iso,
  Mii,
  Complete;

  /** Returns the previous step, or null if this is the first. */
  fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

  companion object {
    val TOTAL: Int = entries.size
  }
}
