package com.skiletro.wheelwitch.util

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.jupiter.api.Test

class DolphinLauncherTest {

    @Test
    fun `generateLaunchJson has correct top-level fields`() {
        val json = DolphinLauncher.generateLaunchJson("/storage", "/path/to/game.iso", "RR")
        val obj = JSONObject(json)

        assertThat(obj.getString("base-file")).isEqualTo("/path/to/game.iso")
        assertThat(obj.getString("display-name")).isEqualTo("RR")
        assertThat(obj.getString("type")).isEqualTo("dolphin-game-mod-descriptor")
        assertThat(obj.getInt("version")).isEqualTo(1)
    }

    @Test
    fun `generateLaunchJson uses default display name`() {
        val json = DolphinLauncher.generateLaunchJson("/s", "/g.iso")
        val obj = JSONObject(json)
        assertThat(obj.getString("display-name")).isEqualTo("RR")
    }

    @Test
    fun `generateLaunchJson builds riivolution structure`() {
        val json = DolphinLauncher.generateLaunchJson("/storage/path", "/iso/mkwii.iso")
        val obj = JSONObject(json)

        val riivolution = obj.getJSONObject("riivolution")
        val patches = riivolution.getJSONArray("patches")
        assertThat(patches.length()).isEqualTo(1)

        val patch = patches.getJSONObject(0)
        assertThat(patch.getString("root")).isEqualTo("/storage/path")
        assertThat(patch.getString("xml")).isEqualTo("/storage/path/riivolution/RetroRewind6.xml")

        val options: JSONArray = patch.getJSONArray("options")
        assertThat(options.length()).isEqualTo(3)
    }

    @Test
    fun `generateLaunchJson options have correct choice and names`() {
        val json = DolphinLauncher.generateLaunchJson("/s", "/g.iso")
        val obj = JSONObject(json)
        val options = obj
            .getJSONObject("riivolution")
            .getJSONArray("patches")
            .getJSONObject(0)
            .getJSONArray("options")

        val expectedOptions = listOf(
            Triple(1, "Pack", "Retro Rewind"),
            Triple(2, "My Stuff", "Retro Rewind"),
            Triple(2, "Seperate Savegame", "Retro Rewind"),
        )

        for ((i, expected) in expectedOptions.withIndex()) {
            val opt = options.getJSONObject(i)
            assertThat(opt.getInt("choice")).isEqualTo(expected.first)
            assertThat(opt.getString("option-name")).isEqualTo(expected.second)
            assertThat(opt.getString("section-name")).isEqualTo(expected.third)
        }
    }

    @Test
    fun `generateLaunchJson produces valid JSON with all required fields`() {
        val json = DolphinLauncher.generateLaunchJson("/data/retro", "/data/roms/mkw.iso", "MKW-RR")
        val obj = JSONObject(json)

        assertThat(obj.has("base-file")).isTrue()
        assertThat(obj.has("display-name")).isTrue()
        assertThat(obj.has("riivolution")).isTrue()
        assertThat(obj.has("type")).isTrue()
        assertThat(obj.has("version")).isTrue()
    }

    @Test
    fun `readIsoPathFromLaunchJson returns null when file missing`() {
        val result = DolphinLauncher.readIsoPathFromLaunchJson("/nonexistent/path")
        assertThat(result).isNull()
    }

    @Test
    fun `readIsoPathFromLaunchJson parses base-file from valid RR json`() {
        val dir = createTempDir()
        try {
            val rrJson = JSONObject().apply {
                put("base-file", "/my/game.iso")
                put("display-name", "RR")
            }.toString(2)
            java.io.File(dir, "RR.json").writeText(rrJson)

            val result = DolphinLauncher.readIsoPathFromLaunchJson(dir.absolutePath)
            assertThat(result).isEqualTo("/my/game.iso")
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `readIsoPathFromLaunchJson returns null for empty base-file`() {
        val dir = createTempDir()
        try {
            val rrJson = JSONObject().apply {
                put("base-file", "")
            }.toString(2)
            java.io.File(dir, "RR.json").writeText(rrJson)

            val result = DolphinLauncher.readIsoPathFromLaunchJson(dir.absolutePath)
            assertThat(result).isNull()
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `readIsoPathFromLaunchJson returns null for malformed JSON`() {
        val dir = createTempDir()
        try {
            java.io.File(dir, "RR.json").writeText("not valid json")

            val result = DolphinLauncher.readIsoPathFromLaunchJson(dir.absolutePath)
            assertThat(result).isNull()
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test
    fun `generateLaunchJson omits My Stuff option when mode is Disabled`() {
        val json = DolphinLauncher.generateLaunchJson(
            "/s",
            "/g.iso",
            myStuffMode = DolphinLauncher.MyStuffMode.Disabled
        )
        val options = JSONObject(json)
            .getJSONObject("riivolution")
            .getJSONArray("patches")
            .getJSONObject(0)
            .getJSONArray("options")

        assertThat(options.length()).isEqualTo(2)
        assertThat(options.getJSONObject(0).getString("option-name")).isEqualTo("Pack")
        assertThat(options.getJSONObject(1).getString("option-name")).isEqualTo("Seperate Savegame")
    }

    @Test
    fun `generateLaunchJson uses choice 2 for My Stuff when mode is Everything`() {
        val json = DolphinLauncher.generateLaunchJson(
            "/s",
            "/g.iso",
            myStuffMode = DolphinLauncher.MyStuffMode.Everything
        )
        val options = JSONObject(json)
            .getJSONObject("riivolution")
            .getJSONArray("patches")
            .getJSONObject(0)
            .getJSONArray("options")

        val myStuff = options.getJSONObject(1)
        assertThat(myStuff.getInt("choice")).isEqualTo(2)
        assertThat(myStuff.getString("option-name")).isEqualTo("My Stuff")
    }

    @Test
    fun `generateLaunchJson uses choice 4 for My Stuff when mode is MusicOnly`() {
        val json = DolphinLauncher.generateLaunchJson(
            "/s",
            "/g.iso",
            myStuffMode = DolphinLauncher.MyStuffMode.MusicOnly
        )
        val options = JSONObject(json)
            .getJSONObject("riivolution")
            .getJSONArray("patches")
            .getJSONObject(0)
            .getJSONArray("options")

        val myStuff = options.getJSONObject(1)
        assertThat(myStuff.getInt("choice")).isEqualTo(4)
        assertThat(myStuff.getString("option-name")).isEqualTo("My Stuff")
    }

    private fun createTempDir() = java.io.File.createTempFile("dolphin_test", "").also {
        it.delete()
        it.mkdirs()
    }
}
