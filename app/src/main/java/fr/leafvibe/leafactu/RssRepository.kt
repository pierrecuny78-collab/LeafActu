package fr.leafvibe.leafactu

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

data class LiveArticle(
    val title: String,
    val summary: String,
    val link: String,
    val published: String,
    val source: String
)

object RssRepository {
    private data class FeedSource(val name: String, val url: String)

    // Première sélection de flux publics ; chaque flux reste identifiable.
    private val sources = listOf(
        FeedSource("France 24", "https://www.france24.com/fr/rss"),
        FeedSource("Franceinfo", "https://www.francetvinfo.fr/titres.rss")
    )

    suspend fun loadLatest(): List<LiveArticle> = withContext(Dispatchers.IO) {
        sources.flatMap { source ->
            runCatching { parseFeed(source) }.getOrElse { emptyList() }
        }.distinctBy { it.link.ifBlank { it.title } }
            .sortedByDescending { it.published }
            .take(60)
    }

    private fun parseFeed(source: FeedSource): List<LiveArticle> {
        val connection = (URL(source.url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 12000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "LeafActu/1.1 (Android RSS reader)")
            setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
        }

        try {
            connection.inputStream.use { input ->
                val parser = Xml.newPullParser()
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                parser.setInput(input, null)

                val articles = mutableListOf<LiveArticle>()
                var event = parser.eventType
                var inEntry = false
                var title = ""
                var summary = ""
                var link = ""
                var published = ""

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name.lowercase()) {
                                "item", "entry" -> {
                                    inEntry = true
                                    title = ""; summary = ""; link = ""; published = ""
                                }
                                "title" -> if (inEntry) title = parser.nextText().trim()
                                "description", "summary", "content" ->
                                    if (inEntry && summary.isBlank()) summary = parser.nextText().trim()
                                "link" -> if (inEntry) {
                                    val href = parser.getAttributeValue(null, "href")
                                    link = (href ?: parser.nextText()).trim()
                                }
                                "pubdate", "published", "updated" ->
                                    if (inEntry) published = parser.nextText().trim()
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (parser.name.equals("item", true) || parser.name.equals("entry", true)) {
                                if (title.isNotBlank()) {
                                    articles.add(LiveArticle(
                                        title = title,
                                        summary = summary.replace(Regex("<[^>]*>"), " ").take(420),
                                        link = link,
                                        published = published,
                                        source = source.name
                                    ))
                                }
                                inEntry = false
                            }
                        }
                    }
                    event = parser.next()
                }
                articles
            }
        } finally {
            connection.disconnect()
        }
    }
}
