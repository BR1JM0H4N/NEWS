package com.mohan.news.network

import com.mohan.news.data.ReaderArticle
import com.mohan.news.data.ReaderBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI
import java.util.concurrent.TimeUnit

sealed class ReaderResult {
    data class Success(val article: ReaderArticle) : ReaderResult()
    data class Error(val message: String, val originalUrl: String) : ReaderResult()
}

/**
 * Fetches the HTML for a news link (resolving the Google News redirect where
 * needed) and extracts a clean, readable version of the article: title,
 * byline, lead image, and body content, in original order, with all ads,
 * navigation, scripts, and other page chrome stripped out — similar in spirit
 * to Mozilla's Readability.js, implemented here with Jsoup.
 */
object ArticleReaderRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private const val BROWSER_UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    suspend fun fetchArticle(
        link: String,
        fallbackTitle: String,
        fallbackSource: String
    ): ReaderResult = withContext(Dispatchers.IO) {
        if (link.isBlank()) return@withContext ReaderResult.Error("No link available for this story", link)

        try {
            var (finalUrl, html) = fetchHtml(link)

            // Google News article links are sometimes an interstitial page rather
            // than a plain HTTP redirect. Try to pull the real publisher URL out
            // of that page and fetch it directly.
            if (isGoogleNewsHost(finalUrl)) {
                extractRedirectTarget(html)?.let { resolved ->
                    if (!isGoogleNewsHost(resolved)) {
                        val second = fetchHtml(resolved)
                        finalUrl = second.first
                        html = second.second
                    }
                }
            }

            if (html.isBlank()) {
                return@withContext ReaderResult.Error("Couldn't load this article", finalUrl)
            }

            val doc = Jsoup.parse(html, finalUrl)
            val article = extract(doc, finalUrl, fallbackTitle, fallbackSource)

            if (article.blocks.none { it is ReaderBlock.Paragraph }) {
                return@withContext ReaderResult.Error(
                    "This publisher's page couldn't be read in-app",
                    finalUrl
                )
            }

            ReaderResult.Success(article)
        } catch (e: Exception) {
            ReaderResult.Error(e.message ?: "Couldn't load this article", link)
        }
    }

    private fun fetchHtml(url: String): Pair<String, String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_UA)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val landedUrl = response.request.url.toString()
            return landedUrl to body
        }
    }

    private fun isGoogleNewsHost(url: String): Boolean = url.contains("news.google.")

    private fun extractRedirectTarget(html: String): String? {
        if (html.isBlank()) return null
        val doc = Jsoup.parse(html)

        // <meta http-equiv="refresh" content="0;URL='https://...'">
        val refreshContent = doc.select("meta[http-equiv=refresh]").firstOrNull()?.attr("content")
        if (!refreshContent.isNullOrBlank()) {
            Regex("url=(.+)", RegexOption.IGNORE_CASE).find(refreshContent)?.groupValues?.get(1)
                ?.trim('\'', '"', ' ')
                ?.takeIf { it.startsWith("http") }
                ?.let { return it }
        }

        // Fall back to the first outbound link that isn't Google News itself.
        return doc.select("a[href^=http]").firstOrNull { !isGoogleNewsHost(it.attr("href")) }?.attr("href")
    }

    // ---- extraction ----

    private const val NOISE_SELECTOR =
        "script, style, noscript, iframe, form, svg, button, input, template, link, " +
            "nav, footer, aside, header, " +
            "[class*=advert], [id*=advert], [class*=ad-], [id*=ad-], [class*=cookie], " +
            "[class*=newsletter], [class*=subscribe], [class*=share], [class*=social], " +
            "[class*=comment], [id*=comment], [class*=sidebar], [class*=related], " +
            "[class*=popup], [class*=paywall], [class*=promo], [aria-hidden=true]"

    private fun extract(doc: Document, finalUrl: String, fallbackTitle: String, fallbackSource: String): ReaderArticle {
        val title = doc.select("meta[property=og:title]").attr("content").ifBlank {
            doc.title().ifBlank { fallbackTitle }
        }
        val siteName = doc.select("meta[property=og:site_name]").attr("content").ifBlank {
            fallbackSource.ifBlank { hostOf(finalUrl) }
        }
        val leadImage = doc.select("meta[property=og:image]").attr("content").ifBlank { null }
        val byline = (
            doc.select("meta[name=author]").attr("content").ifBlank {
                doc.select("[rel=author], .byline, .author-name").firstOrNull()?.text().orEmpty()
            }
            ).takeIf { it.isNotBlank() }
        val published = doc.select("meta[property=article:published_time]").attr("content").ifBlank {
            doc.select("time[datetime]").firstOrNull()?.attr("datetime").orEmpty()
        }.takeIf { it.isNotBlank() }

        val body = doc.body()
        body?.select(NOISE_SELECTOR)?.remove()

        val container = findBestContainer(body) ?: body

        val blocks = mutableListOf<ReaderBlock>()
        val seen = mutableSetOf<String>()

        container?.select("h1, h2, h3, h4, p, blockquote, figure, img")?.forEach { el ->
            when (el.tagName()) {
                "h1", "h2", "h3", "h4" -> {
                    val text = el.text().trim()
                    if (text.length in 3..200 && seen.add("H:$text")) {
                        blocks.add(ReaderBlock.Heading(text))
                    }
                }
                "p" -> {
                    val text = el.text().trim()
                    if (text.length >= 40 && seen.add(text)) {
                        blocks.add(ReaderBlock.Paragraph(text))
                    }
                }
                "blockquote" -> {
                    val text = el.text().trim()
                    if (text.length >= 20 && seen.add("Q:$text")) {
                        blocks.add(ReaderBlock.Quote(text))
                    }
                }
                "figure" -> {
                    val img = el.selectFirst("img")
                    val src = img?.let { absoluteImageUrl(it) }
                    if (src != null && !isJunkImage(img) && seen.add("I:$src")) {
                        val caption = el.selectFirst("figcaption")?.text()?.trim()?.takeIf { it.isNotBlank() }
                        blocks.add(ReaderBlock.Image(src, caption))
                    }
                }
                "img" -> {
                    if (el.closest("figure") != null) return@forEach // already handled above
                    val src = absoluteImageUrl(el)
                    if (src != null && !isJunkImage(el) && seen.add("I:$src")) {
                        blocks.add(ReaderBlock.Image(src, null))
                    }
                }
            }
        }

        return ReaderArticle(
            title = cleanText(title),
            sourceName = cleanText(siteName),
            byline = byline?.let { cleanText(it) },
            publishedDate = published,
            leadImageUrl = leadImage,
            canonicalUrl = finalUrl,
            blocks = blocks
        )
    }

    private fun findBestContainer(body: Element?): Element? {
        if (body == null) return null
        var best: Element? = null
        var bestScore = 0.0
        for (candidate in body.select("article, div, section, main")) {
            val paraTextLen = candidate.select("p").sumOf { it.text().length }
            if (paraTextLen < 200) continue
            var score = paraTextLen.toDouble()
            val classAndId = (candidate.className() + " " + candidate.id()).lowercase()
            if (Regex("article|content|post|story|entry|main").containsMatchIn(classAndId)) score *= 1.4
            if (Regex("comment|sidebar|footer|nav|menu|related|share|promo|widget").containsMatchIn(classAndId)) score *= 0.2
            score *= (1 - linkTextDensity(candidate)).coerceIn(0.1, 1.0)
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        return best ?: body.selectFirst("article")
    }

    private fun linkTextDensity(el: Element): Double {
        val totalText = el.text().length
        if (totalText == 0) return 0.0
        val linkText = el.select("a").sumOf { it.text().length }
        return linkText.toDouble() / totalText.toDouble()
    }

    private fun absoluteImageUrl(img: Element): String? {
        val src = img.attr("abs:src").ifBlank { img.attr("abs:data-src") }
        return src.ifBlank { null }
    }

    private fun isJunkImage(img: Element): Boolean {
        val src = (img.attr("abs:src").ifBlank { img.attr("abs:data-src") }).lowercase()
        if (listOf("sprite", "icon", "logo", "avatar", "pixel", "1x1", "spacer", "tracking").any { src.contains(it) }) {
            return true
        }
        val w = img.attr("width").toIntOrNull()
        val h = img.attr("height").toIntOrNull()
        if ((w != null && w in 1..40) || (h != null && h in 1..40)) return true
        return false
    }

    private fun hostOf(url: String): String = try {
        URI(url).host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }

    private fun cleanText(s: String): String = s.replace(Regex("\\s+"), " ").trim()
}
