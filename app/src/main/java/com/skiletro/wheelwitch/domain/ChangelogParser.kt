package com.skiletro.wheelwitch.domain

import android.content.Context
import com.skiletro.wheelwitch.model.ChangelogEntry
import com.skiletro.wheelwitch.util.HttpClientProvider
import com.skiletro.wheelwitch.util.PrefsKeys
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.json.JSONArray
import org.json.JSONObject

/** Fetches and parses the Retro Rewind version history from the Tockdom wiki. */
object ChangelogParser {

    private const val WIKI_URL = "https://wiki.tockdom.com/wiki/Retro_Rewind"
    private val httpClient get() = HttpClientProvider.client

    private const val PREFS_NAME = "changelog_cache"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_VERSION = "version"

    /** Fetches changelog, using cached data if the server version hasn't changed. */
    fun fetchWithCache(context: Context): Result<List<ChangelogEntry>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val cachedVersion = prefs.getString(KEY_VERSION, null)
        val serverVersion = context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PrefsKeys.LAST_SERVER_VERSION_KEY, null)

        if (cachedVersion != null && cachedVersion == serverVersion) {
            val cached = readCache(prefs)
            if (cached != null) return Result.success(cached)
        }

        val result = fetch()
        result.onSuccess { entries ->
            writeCache(prefs, entries, serverVersion)
        }
        return result
    }

    private fun readCache(prefs: android.content.SharedPreferences): List<ChangelogEntry>? {
        val json = prefs.getString(KEY_ENTRIES, null) ?: return null
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                ChangelogEntry(
                    version = obj.getString("version"),
                    date = obj.getString("date"),
                    changes = obj.getJSONArray("changes").let { changes ->
                        (0 until changes.length()).map { changes.getString(it) }
                    }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeCache(
        prefs: android.content.SharedPreferences,
        entries: List<ChangelogEntry>,
        version: String?
    ) {
        val arr = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("version", entry.version)
                put("date", entry.date)
                put("changes", JSONArray(entry.changes))
            }
            arr.put(obj)
        }
        prefs.edit()
            .putString(KEY_ENTRIES, arr.toString())
            .putString(KEY_VERSION, version)
            .apply()
    }

    /** Fetches the wiki page via OkHttp (browser UA to avoid 403) and parses all version history tables. */
    fun fetch(): Result<List<ChangelogEntry>> = runCatching {
        val request = Request.Builder()
            .url(WIKI_URL)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .build()
        val html = httpClient.newCall(request).execute().use { response ->
            response.body?.string() ?: error("Empty response from wiki")
        }
        val doc = Jsoup.parse(html)
        parse(doc)
    }

    /** Parses a Jsoup [Document] for all `table.wikitable` elements after the `#Version_History` heading. Handles both `.spoilers`-wrapped and bare tables. Results are reversed so latest version is first. */
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
