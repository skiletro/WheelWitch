package com.skiletro.wheelwitch.ui.screens.onboarding

/** Reduced onboarding flow. The full picker / permission / install steps have been ripped out. */
internal enum class OnboardingStep {
  Welcome,
  Beta,
  Complete;

  /** Returns the previous step, or null if this is the first. */
  fun previous(): OnboardingStep? = entries.getOrNull(ordinal - 1)

  companion object {
    val TOTAL: Int = entries.size
  }
}
