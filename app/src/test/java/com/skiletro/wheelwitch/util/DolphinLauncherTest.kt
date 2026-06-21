package com.skiletro.wheelwitch.util

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.jupiter.api.Test

class DolphinLauncherTest {

    @Test
    fun `launchDolphin returns a failure with NotImplementedError for now`() {
        // The real launch flow is ripped out for a planned rewrite.
        val result = DolphinLauncher.launchDolphin(
            context = mockk<Context>(relaxed = true),
            jsonFilePath = "/x/RR.json",
        )
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
    }

    @Test
    fun `DOLPHIN_PACKAGE constant is preserved`() {
        // Used by MiiWadInstaller.launchWadFile; must not change.
        assertThat(DolphinLauncher.DOLPHIN_PACKAGE).isEqualTo("org.dolphinemu.dolphinemu")
    }

    @Test
    fun `DOLPHIN_MAIN_ACTIVITY constant is preserved`() {
        assertThat(DolphinLauncher.DOLPHIN_MAIN_ACTIVITY)
            .isEqualTo("org.dolphinemu.dolphinemu.ui.main.MainActivity")
    }

    @Test
    fun `RR_JSON_NAME constant is preserved`() {
        // The Riivolution XML still references this filename via the future rewrite.
        assertThat(DolphinLauncher.RR_JSON_NAME).isEqualTo("RR.json")
    }
}
