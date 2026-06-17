package com.skiletro.wheelwitch.domain

import com.skiletro.wheelwitch.model.ChangelogEntry
import com.skiletro.wheelwitch.util.HttpClientProvider
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

object ChangelogParser {

    private const val WIKI_URL = "https://wiki.tockdom.com/wiki/Retro_Rewind"
    private val client get() = HttpClientProvider.client

    fun fetch(): Result<List<ChangelogEntry>> = runCatching {
        val request = Request.Builder()
            .url(WIKI_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val html = client.newCall(request).execute().use { response ->
            response.body?.string() ?: error("Empty response from wiki")
        }
        val doc = Jsoup.parse(html)
        parse(doc)
    }

    fun parse(doc: Document): List<ChangelogEntry> {
        val entries = mutableListOf<ChangelogEntry>()
        val historyHeading = doc.selectFirst("span#Version_History") ?: return entries
        var sibling = historyHeading.parent()?.nextElementSibling()
        while (sibling != null) {
            val table = sibling.selectFirst("table.wikitable")
            if (table != null) {
                val rows = table.select("tbody tr")
                for (row in rows) {
                    val tds = row.select("td")
                    if (tds.size < 3) continue
                    val version = tds[0].text().trim()
                    val date = tds[1].text().trim()
                    val infoTd = tds[2]
                    val changes = parseChanges(infoTd)
                    if (version.isNotBlank()) {
                        entries.add(ChangelogEntry(version, date, changes))
                    }
                }
            }
            sibling = sibling.nextElementSibling()
        }
        return entries.reversed()
    }

    private fun parseChanges(td: org.jsoup.nodes.Element): List<String> {
        val listItems = td.select("li")
        if (listItems.isNotEmpty()) {
            return listItems.map { it.text().trim() }.filter { it.isNotBlank() }
        }
        val text = td.text().trim()
        return if (text.isNotBlank()) listOf(text) else emptyList()
    }
}
