package com.skiletro.wheelwitch.network

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class VersionFileParserTest {

    @Test
    fun `parseUpdatesText parses valid manifest`() {
        val text = """
            3.2.5 https://example.com/updates/3.2.5.zip /storage/path Adds feature A
            3.2.6 https://example.com/updates/3.2.6.zip /storage/path2 Fixes bug B
        """.trimIndent()

        val updates = parseUpdatesText(text)
        assertThat(updates).hasSize(2)

        assertThat(updates[0].version.major).isEqualTo(3)
        assertThat(updates[0].version.minor).isEqualTo(2)
        assertThat(updates[0].version.patch).isEqualTo(5)
        assertThat(updates[0].url).isEqualTo("https://example.com/updates/3.2.5.zip")
        assertThat(updates[0].path).isEqualTo("/storage/path")
        assertThat(updates[0].description).isEqualTo("Adds feature A")

        assertThat(updates[1].version.toString()).isEqualTo("3.2.6")
        assertThat(updates[1].url).isEqualTo("https://example.com/updates/3.2.6.zip")
    }

    @Test
    fun `parseUpdatesText skips malformed lines`() {
        val text = """
            3.2.5 https://example.com/a.zip /path desc
            bad_line_without_enough_parts
            3.2.6 https://example.com/b.zip /path2 desc2
        """.trimIndent()

        val updates = parseUpdatesText(text)
        assertThat(updates).hasSize(2)
        assertThat(updates[0].version.toString()).isEqualTo("3.2.5")
        assertThat(updates[1].version.toString()).isEqualTo("3.2.6")
    }

    @Test
    fun `parseUpdatesText skips lines with invalid version`() {
        val text = """
            3.2.5 https://a.zip /path desc
            x.y.z https://b.zip /path2 desc2
            3.2.6 https://c.zip /path3 desc3
        """.trimIndent()

        val updates = parseUpdatesText(text)
        assertThat(updates).hasSize(2)
    }

    @Test
    fun `parseUpdatesText returns empty for blank input`() {
        assertThat(parseUpdatesText("")).isEmpty()
        assertThat(parseUpdatesText("   ")).isEmpty()
        assertThat(parseUpdatesText("\n\n\n")).isEmpty()
    }

    @Test
    fun `parseDeletionsText parses valid deletion manifest`() {
        val text = """
            3.2.5 /riivolution/old/file.txt
            3.2.6 /riivolution/another.ark
        """.trimIndent()

        val deletions = parseDeletionsText(text)
        assertThat(deletions).hasSize(2)

        assertThat(deletions[0].version.toString()).isEqualTo("3.2.5")
        assertThat(deletions[0].path).isEqualTo("/riivolution/old/file.txt")

        assertThat(deletions[1].version.toString()).isEqualTo("3.2.6")
        assertThat(deletions[1].path).isEqualTo("/riivolution/another.ark")
    }

    @Test
    fun `parseDeletionsText skips malformed lines`() {
        val text = """
            3.2.5 /path/to/file
            orphan_line_without_version
            3.2.6 /another/file
        """.trimIndent()

        val deletions = parseDeletionsText(text)
        assertThat(deletions).hasSize(2)
    }

    @Test
    fun `parseDeletionsText skips lines with invalid version`() {
        val text = """
            3.2.5 /path/file
            bad.version /other/file
            3.2.6 /yet/another
        """.trimIndent()

        val deletions = parseDeletionsText(text)
        assertThat(deletions).hasSize(2)
    }

    @Test
    fun `parseDeletionsText returns empty for blank input`() {
        assertThat(parseDeletionsText("")).isEmpty()
        assertThat(parseDeletionsText("  \n\n  ")).isEmpty()
    }
}
