package com.mohan.news.network

import android.text.Html
import com.mohan.news.data.Article
import com.mohan.news.data.RelatedSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

/**
 * Parses the Google News RSS XML format.
 *
 * Each <item> looks roughly like:
 * <item>
 *   <title>Headline - Source Name</title>
 *   <link>https://news.google.com/rss/articles/....</link>
 *   <pubDate>Wed, 05 Aug 2026 10:00:00 GMT</pubDate>
 *   <source url="https://source.example.com">Source Name</source>
 *   <description>
 *     &lt;ol&gt;&lt;li&gt;&lt;a href="link1"&gt;Headline 1&lt;/a&gt;&amp;nbsp;&lt;font&gt;Source A&lt;/font&gt;&lt;/li&gt;
 *     &lt;li&gt;&lt;a href="link2"&gt;Headline 2&lt;/a&gt;&amp;nbsp;&lt;font&gt;Source B&lt;/font&gt;&lt;/li&gt;&lt;/ol&gt;
 *   </description>
 * </item>
 *
 * The description's inner HTML contains an <ol> of related coverage links for the
 * same story cluster — the first <li> is usually the main article itself and the
 * rest are the "different sources" for the same story.
 */
object RssParser {

    fun parse(xml: String): List<Article> {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xml))

        val articles = mutableListOf<Article>()

        var eventType = parser.eventType
        var inItem = false

        var title = ""
        var link = ""
        var pubDate = ""
        var sourceName = ""
        var description = ""
        var currentTag = ""

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag == "item") {
                        inItem = true
                        title = ""; link = ""; pubDate = ""; sourceName = ""; description = ""
                    } else if (inItem && currentTag == "source") {
                        sourceName = if (parser.next() == XmlPullParser.TEXT) parser.text ?: "" else ""
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inItem) {
                        val text = parser.text ?: ""
                        when (currentTag) {
                            "title" -> title += text
                            "link" -> link += text
                            "pubDate" -> pubDate += text
                            "description" -> description += text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name == "item" && inItem) {
                        inItem = false
                        val related = parseRelatedFromDescription(description, mainTitle = title)
                        articles.add(
                            Article(
                                title = title.trim(),
                                link = link.trim(),
                                pubDate = pubDate.trim(),
                                sourceName = sourceName.trim().ifBlank { guessSourceFromTitle(title) },
                                description = description.trim(),
                                relatedSources = related
                            )
                        )
                    }
                }
            }
            eventType = parser.next()
        }

        return articles
    }

    private fun guessSourceFromTitle(title: String): String {
        val idx = title.lastIndexOf(" - ")
        return if (idx > 0 && idx < title.length - 3) title.substring(idx + 3).trim() else "Unknown"
    }

    /**
     * The description field contains escaped HTML with an <ol><li> list of related
     * coverage. We extract each <li>'s link, headline text, and trailing source name.
     */
    private fun parseRelatedFromDescription(descriptionHtml: String, mainTitle: String): List<RelatedSource> {
        if (descriptionHtml.isBlank()) return emptyList()
        val results = mutableListOf<RelatedSource>()

        // <li><a href="URL" ...>HEADLINE</a>&nbsp;<font ...>SOURCE</font></li>
        val liRegex = Regex("<li>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
        val hrefRegex = Regex("href=\"(.*?)\"")
        val fontRegex = Regex("<font[^>]*>(.*?)</font>", RegexOption.DOT_MATCHES_ALL)

        for (liMatch in liRegex.findAll(descriptionHtml)) {
            val liContent = liMatch.groupValues[1]
            val href = hrefRegex.find(liContent)?.groupValues?.get(1) ?: continue
            val sourceMatch = fontRegex.find(liContent)?.groupValues?.get(1)
            val source = sourceMatch?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim() } ?: ""

            // Headline text is whatever is inside <a>...</a>
            val anchorTextRegex = Regex("<a[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
            val anchorText = anchorTextRegex.find(liContent)?.groupValues?.get(1)?.let {
                Html.fromHtml(it, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } ?: ""

            if (anchorText.isNotBlank() && source.isNotBlank()) {
                // Skip if this is effectively identical to the main title/source (avoid self-duplication)
                results.add(RelatedSource(title = anchorText, sourceName = source, link = href))
            }
        }
        return results
    }
}
