package com.skiletro.wheelwitch.data

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PackStorageTest {

    @Test
    fun `buildPathFromDocId primary storage with simple path`() {
        val path = PackStorage.buildPathFromDocId("primary:RetroRewind6", null)
        assertThat(path).isEqualTo("/storage/emulated/0/RetroRewind6")
    }

    @Test
    fun `buildPathFromDocId primary storage with spaces in path`() {
        val path = PackStorage.buildPathFromDocId("primary:Retro%20Rewind%20Pack", null)
        assertThat(path).isEqualTo("/storage/emulated/0/Retro Rewind Pack")
    }

    @Test
    fun `buildPathFromDocId primary storage with percent-encoded special characters`() {
        val path = PackStorage.buildPathFromDocId("primary:My%20Folder%2FSub", null)
        assertThat(path).isEqualTo("/storage/emulated/0/My Folder/Sub")
    }

    @Test
    fun `buildPathFromDocId raw storage`() {
        val path = PackStorage.buildPathFromDocId("raw:sdcard1/data", null)
        assertThat(path).isEqualTo("/storage/raw/sdcard1/data")
    }

    @Test
    fun `buildPathFromDocId volume storage`() {
        val path = PackStorage.buildPathFromDocId("1A2B-3C4D:Games/Rewind", null)
        assertThat(path).isEqualTo("/storage/1A2B-3C4D/Games/Rewind")
    }

    @Test
    fun `buildPathFromDocId volume storage with spaces`() {
        val path = PackStorage.buildPathFromDocId("1A2B-3C4D:My%20Games", null)
        assertThat(path).isEqualTo("/storage/1A2B-3C4D/My Games")
    }

    @Test
    fun `buildPathFromDocId null docId returns fallback`() {
        val path = PackStorage.buildPathFromDocId(null, "/some/fallback")
        assertThat(path).isEqualTo("/some/fallback")
    }

    @Test
    fun `buildPathFromDocId no colon separator returns fallback`() {
        val path = PackStorage.buildPathFromDocId("nocolon", "/fallback")
        assertThat(path).isEqualTo("/fallback")
    }

    @Test
    fun `buildPathFromDocId primary case insensitive`() {
        val path = PackStorage.buildPathFromDocId("Primary:Test", null)
        assertThat(path).isEqualTo("/storage/emulated/0/Test")
    }

    @Test
    fun `buildPathFromDocId path without percent encoding`() {
        val path = PackStorage.buildPathFromDocId("primary:PlainFolder", null)
        assertThat(path).isEqualTo("/storage/emulated/0/PlainFolder")
    }
}