package com.skiletro.wheelwitch.domain

import com.google.common.truth.Truth.assertThat
import org.jsoup.Jsoup
import org.junit.jupiter.api.Test

class ChangelogParserTest {

    @Test
    fun `parse returns empty when no Version History heading`() {
        val doc = Jsoup.parse("<html><body><p>No history</p></body></html>")
        assertThat(ChangelogParser.parse(doc)).isEmpty()
    }

    @Test
    fun `parse extracts version, date, and changes from table`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <table class="wikitable">
                <tbody>
                  <tr><td>3.2.5</td><td>2025-01-15</td><td><ul><li>Bug A</li><li>Bug B</li></ul></td></tr>
                </tbody>
              </table>
            </body></html>
        """.trimIndent()
        val doc = Jsoup.parse(html)
        val entries = ChangelogParser.parse(doc)
        assertThat(entries).hasSize(1)
        assertThat(entries[0].version).isEqualTo("3.2.5")
        assertThat(entries[0].date).isEqualTo("2025-01-15")
        assertThat(entries[0].changes).containsExactly("Bug A", "Bug B")
    }

    @Test
    fun `parse returns latest first (reversed order)`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <table class="wikitable"><tbody>
                <tr><td>3.0.0</td><td>2024-01-01</td><td>Old</td></tr>
                <tr><td>3.2.5</td><td>2025-01-01</td><td>New</td></tr>
              </tbody></table>
            </body></html>
        """.trimIndent()
        val entries = ChangelogParser.parse(doc = Jsoup.parse(html))
        assertThat(entries.map { it.version }).containsExactly("3.2.5", "3.0.0").inOrder()
    }

    @Test
    fun `parse handles spoilers-wrapped tables`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <div class="spoilers">
                <div class="mw-parser-output">
                  <table class="wikitable"><tbody>
                    <tr><td>3.2.5</td><td>2025-01-01</td><td>hidden change</td></tr>
                  </tbody></table>
                </div>
              </div>
            </body></html>
        """.trimIndent()
        val entries = ChangelogParser.parse(Jsoup.parse(html))
        assertThat(entries).hasSize(1)
        assertThat(entries[0].changes).containsExactly("hidden change")
    }

    @Test
    fun `parse falls back to cell text when no li elements`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <table class="wikitable"><tbody>
                <tr><td>3.2.5</td><td>2025-01-01</td><td>Plain text change</td></tr>
              </tbody></table>
            </body></html>
        """.trimIndent()
        val entries = ChangelogParser.parse(Jsoup.parse(html))
        assertThat(entries[0].changes).containsExactly("Plain text change")
    }

    @Test
    fun `parse skips rows with too few cells`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <table class="wikitable"><tbody>
                <tr><td>only one cell</td></tr>
                <tr><td>3.2.5</td><td>2025-01-01</td><td>valid</td></tr>
              </tbody></table>
            </body></html>
        """.trimIndent()
        val entries = ChangelogParser.parse(Jsoup.parse(html))
        assertThat(entries).hasSize(1)
    }

    @Test
    fun `parse handles multiple tables in order`() {
        val html = """
            <html><body>
              <h2><span id="Version_History">Version History</span></h2>
              <p>Some intro text</p>
              <table class="wikitable"><tbody>
                <tr><td>3.0.0</td><td>2024-01-01</td><td>Oldest</td></tr>
              </tbody></table>
              <p>More text</p>
              <table class="wikitable"><tbody>
                <tr><td>3.2.5</td><td>2025-01-01</td><td>Newer</td></tr>
              </tbody></table>
            </body></html>
        """.trimIndent()
        val entries = ChangelogParser.parse(Jsoup.parse(html))
        assertThat(entries.map { it.version }).containsExactly("3.2.5", "3.0.0").inOrder()
    }
}
