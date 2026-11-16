package fr.leafvibe.leafactu

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParser
import java.net.HttpURLConnection
import java.net.URL

data class LiveArticle(
    val title: String,
    val summary: String,
    val link: String,
    val published: String,
    val source: String,
    val category: String,
    val region: String,
    val imageUrl: String? = null
)

object RssRepository {
    private data class FeedSource(val name: String, val url: String)

    // Première sélection de flux publics ; chaque flux reste identifiable.
    private val sources = listOf(
        FeedSource("France 24", "https://www.france24.com/fr/rss"),
        FeedSource("Franceinfo", "https://www.francetvinfo.fr/titres.rss"),
        FeedSource("RFI", "https://www.rfi.fr/fr/rss"),
        FeedSource("Le Monde", "https://www.lemonde.fr/rss/une.xml"),
        FeedSource("BBC News - World", "https://feeds.bbci.co.uk/news/world/rss.xml"),
        FeedSource("The Guardian - World", "https://www.theguardian.com/world/rss"),
        FeedSource("Euronews", "https://www.euronews.com/rss?format=mrss&level=theme&name=news"),
        FeedSource("Les Échos", "https://www.lesechos.fr/rss/rss_une.xml"),
        FeedSource("Libération", "https://www.liberation.fr/arc/outboundfeeds/rss-all/?outputType=xml"),
        FeedSource("Le Figaro", "https://www.lefigaro.fr/rss/figaro_actualites.xml"),
        FeedSource("Numerama", "https://www.numerama.com/feed/"),
        FeedSource("01net", "https://www.01net.com/rss/actualites/"),
        FeedSource("Futura", "https://www.futura-sciences.com/rss/actualites.xml"),
        FeedSource("NASA", "https://www.nasa.gov/news-release/feed/"),
        FeedSource("ScienceDaily", "https://www.sciencedaily.com/rss/all.xml"),
        FeedSource("ESPN", "https://www.espn.com/espn/rss/news")
    )

    suspend fun loadLatest(): List<LiveArticle> = withContext(Dispatchers.IO) {
        coroutineScope {
            sources.map { source ->
                async {
                    withTimeoutOrNull(9000L) {
                        runCatching { parseFeed(source) }.getOrElse { emptyList() }
                    } ?: emptyList()
                }
            }.awaitAll().flatten()
        }.distinctBy { it.link.ifBlank { it.title } }
            .sortedByDescending { it.published }
            .take(100)
    }

    private fun extractImage(html: String): String? {
        val match = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(html) ?: return null
        return match.groupValues.getOrNull(1)?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }

    private fun classifyCategory(title: String, summary: String): String {
        val text = "$title $summary".lowercase()
        val rules = listOf(
            "Sport" to listOf("football", "sport", "match", "championnat", "olympique", "tennis", "rugby"),
            "Technologie" to listOf("technologie", "numérique", "intelligence artificielle", "logiciel", "cyber", "smartphone", "robot"),
            "Sciences" to listOf("science", "recherche", "scientifique", "espace", "nasa", "astronomie", "médecine"),
            "Économie" to listOf("économie", "entreprise", "bourse", "marché", "inflation", "emploi", "budget"),
            "Environnement" to listOf("climat", "environnement", "écologie", "pollution", "biodiversité", "énergie"),
            "Culture" to listOf("culture", "cinéma", "musique", "festival", "livre", "exposition", "art"),
            "Politique" to listOf("gouvernement", "président", "élection", "parlement", "ministre", "politique", "diplomatie")
        )
        return rules.firstOrNull { (_, keywords) -> keywords.any(text::contains) }?.first ?: "Monde"
    }

    private fun classifyRegion(title: String, summary: String): String {
        val text = "$title $summary".lowercase()
        return when {
            listOf("alsace", "strasbourg", "colmar", "mulhouse", "bas-rhin", "haut-rhin").any(text::contains) -> "Alsace"
            listOf("france", "paris", "lyon", "marseille", "bordeaux", "toulouse", "lille").any(text::contains) -> "France"
            listOf("allemagne", "berlin", "munich", "hambourg").any(text::contains) -> "Allemagne"
            listOf("europe", "bruxelles", "union européenne").any(text::contains) -> "Europe"
            listOf("états-unis", "washington", "new york", "amérique").any(text::contains) -> "Amérique"
            listOf("chine", "japon", "inde", "asie", "corée").any(text::contains) -> "Asie"
            listOf("afrique", "nigeria", "kenya", "sénégal", "maroc").any(text::contains) -> "Afrique"
            else -> "Monde"
        }
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
            return connection.inputStream.use { input ->
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
                var imageUrl: String? = null

                while (event != XmlPullParser.END_DOCUMENT) {
                    when (event) {
                        XmlPullParser.START_TAG -> {
                            when (parser.name.lowercase()) {
                                "item", "entry" -> {
                                    inEntry = true
                                    title = ""; summary = ""; link = ""; published = ""; imageUrl = null
                                }
                                "title" -> if (inEntry) title = parser.nextText().trim()
                                "description", "summary", "content" ->
                                    if (inEntry && summary.isBlank()) summary = parser.nextText().trim()
                                "link" -> if (inEntry) {
                                    val href = parser.getAttributeValue(null, "href")
                                    val rel = parser.getAttributeValue(null, "rel")
                                    if (!href.isNullOrBlank()) {
                                        if (rel == "enclosure" && imageUrl == null) imageUrl = href
                                        else if (link.isBlank() && (rel == null || rel == "alternate")) link = href
                                    } else link = parser.nextText().trim()
                                }
                                "enclosure", "media:thumbnail", "media:content", "thumbnail" -> if (inEntry && imageUrl == null) {
                                    val candidate = parser.getAttributeValue(null, "url")
                                    if (!candidate.isNullOrBlank()) imageUrl = candidate
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
                                        source = source.name,
                                        category = classifyCategory(title, summary),
                                        region = classifyRegion(title, summary),
                                        imageUrl = imageUrl ?: extractImage(summary)
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
