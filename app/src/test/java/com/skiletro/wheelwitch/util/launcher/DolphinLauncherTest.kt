package com.skiletro.wheelwitch.util.launcher

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.json.JSONObject
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

    // --- buildLaunchJson ------------------------------------------------

    @Test
    fun `buildLaunchJson produces a parseable JSON object`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        // Smoke test — the body must be valid JSON. org.json.JSONObject
        // throws on malformed input, so reaching the next assertion is
        // the test.
        val obj = JSONObject(json)
        assertThat(obj.length()).isAtLeast(5)
    }

    @Test
    fun `buildLaunchJson top-level field names match the working reference`() {
        // Field-names regression guard for the
        // dolphin-game-mod-descriptor schema. If this fails, Dolphin's
        // parser will likely reject the descriptor. Note: the
        // org.json library does not preserve insertion order, so we
        // assert the key set, not the key order.
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val obj = JSONObject(json)
        assertThat(jsonKeySet(obj))
            .containsExactly("base-file", "display-name", "riivolution", "type", "version")
    }

    @Test
    fun `buildLaunchJson writes the matellush reference top-level values`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/User/Wii/WheelWitch/rom/RMCP01.iso",
                packRootPath = "/storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/User/Wii/WheelWitch/pack",
            )
        val obj = JSONObject(json)
        assertThat(obj.getString("base-file"))
            .isEqualTo(
                "/storage/emulated/0/Android/data/org.dolphinemu.dolphinemu/files/User/Wii/WheelWitch/rom/RMCP01.iso"
            )
        assertThat(obj.getString("display-name")).isEqualTo("Retro Rewind")
        assertThat(obj.getString("type")).isEqualTo("dolphin-game-mod-descriptor")
        // version must be a JSON integer (1), not the string "1".
        assertThat(obj.getInt("version")).isEqualTo(1)
    }

    @Test
    fun `buildLaunchJson patch field names match the working reference`() {
        // Patch field names: options, root, xml.
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val patch =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
        assertThat(jsonKeySet(patch)).containsExactly("options", "root", "xml")
    }

    @Test
    fun `buildLaunchJson writes root and xml from the same packRootPath`() {
        // Path consistency invariant (PLAN §8). root and xml share
        // packRootPath as their prefix, so a single physicalRoot input
        // guarantees the two agree. base-file is the caller's
        // responsibility — see the buildLaunchJson kdoc.
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val patch =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
        assertThat(patch.getString("root")).isEqualTo("/r/pack")
        assertThat(patch.getString("xml")).isEqualTo("/r/pack/riivolution/RetroRewind6.xml")
    }

    @Test
    fun `buildLaunchJson joins xmlRelPath to packRootPath with a single forward slash`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
                xmlRelPath = "riivolution/SubDir/Mod.xml",
            )
        val patch =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
        assertThat(patch.getString("xml")).isEqualTo("/r/pack/riivolution/SubDir/Mod.xml")
    }

    @Test
    fun `buildLaunchJson writes the default Pack1 MyStuff2 Savegame2 options`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val options =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
                .getJSONArray("options")
        assertThat(options.length()).isEqualTo(3)
        // Each option's field set is choice, option-name, section-name.
        for (i in 0 until options.length()) {
            assertThat(jsonKeySet(options.getJSONObject(i)))
                .containsExactly("choice", "option-name", "section-name")
        }
        assertOption(options.getJSONObject(0), name = "Pack", choice = 1)
        assertOption(options.getJSONObject(1), name = "My Stuff", choice = 2)
        assertOption(options.getJSONObject(2), name = "Separate Savegame", choice = 2)
    }

    @Test
    fun `buildLaunchJson customizes choices via parameters`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
                packChoice = 5,
                myStuffChoice = 6,
                separateSavegameChoice = 7,
            )
        val options =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
                .getJSONArray("options")
        assertOption(options.getJSONObject(0), name = "Pack", choice = 5)
        assertOption(options.getJSONObject(1), name = "My Stuff", choice = 6)
        assertOption(options.getJSONObject(2), name = "Separate Savegame", choice = 7)
    }

    @Test
    fun `buildLaunchJson writes every section-name as the display name`() {
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val options =
            JSONObject(json)
                .getJSONObject("riivolution")
                .getJSONArray("patches")
                .getJSONObject(0)
                .getJSONArray("options")
        for (i in 0 until options.length()) {
            assertThat(options.getJSONObject(i).getString("section-name"))
                .isEqualTo("Retro Rewind")
        }
    }

    @Test
    fun `buildLaunchJson writes a single patch in the patches array`() {
        // The matellush reference is a single-patch descriptor. If we
        // ever fan out to multi-patch, the JSON path tests will need
        // an index — call out the constraint here.
        val json =
            DolphinLauncher.buildLaunchJson(
                baseFilePath = "/r/rom.iso",
                packRootPath = "/r/pack",
            )
        val patches =
            JSONObject(json).getJSONObject("riivolution").getJSONArray("patches")
        assertThat(patches.length()).isEqualTo(1)
    }

    private fun assertOption(obj: JSONObject, name: String, choice: Int) {
        assertThat(obj.getString("option-name")).isEqualTo(name)
        assertThat(obj.getInt("choice")).isEqualTo(choice)
    }

    private fun jsonKeySet(obj: JSONObject): Set<String> {
        val out = mutableSetOf<String>()
        val it = obj.keys()
        while (it.hasNext()) out.add(it.next())
        return out
    }
}
