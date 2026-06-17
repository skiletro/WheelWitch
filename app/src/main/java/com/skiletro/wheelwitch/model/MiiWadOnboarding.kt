package com.skiletro.wheelwitch.model

/**
 * State of the Mii Channel WAD install step in the onboarding flow.
 */
sealed class MiiWadOnboarding {
    data object NotInstalled : MiiWadOnboarding()
    data object Installing : MiiWadOnboarding()
    data object Installed : MiiWadOnboarding()
    data class Error(val message: String) : MiiWadOnboarding()
}
