package com.mohan.news.data

/**
 * A single related-coverage source link found nested inside a Google News
 * headline item (the "<source>" siblings inside the description's <ol><li> list,
 * or in some feed variants inside the same <item> as sub-links).
 */
data class RelatedSource(
    val title: String,
    val sourceName: String,
    val link: String
)

/**
 * A single news article/headline parsed from Google News RSS.
 */
data class Article(
    val title: String,
    val link: String,
    val pubDate: String,
    val sourceName: String,
    val description: String = "",
    val relatedSources: List<RelatedSource> = emptyList()
) {
    /** Best-effort clean headline title without the " - Source Name" suffix Google News appends. */
    val cleanTitle: String
        get() {
            val idx = title.lastIndexOf(" - ")
            return if (idx > 0) title.substring(0, idx).trim() else title
        }
}

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class NewsCountry(val code: String, val displayName: String, val language: String)

data class NewsCategory(val id: String, val displayName: String, val topicParam: String?)

object NewsCatalog {
    // Country code (gl), language (hl) pairing used by Google News RSS.
    val countries = listOf(
        NewsCountry("US", "United States", "en-US"),
        NewsCountry("GB", "United Kingdom", "en-GB"),
        NewsCountry("IN", "India", "en-IN"),
        NewsCountry("CA", "Canada", "en-CA"),
        NewsCountry("AU", "Australia", "en-AU"),
        NewsCountry("DE", "Germany", "de"),
        NewsCountry("FR", "France", "fr"),
        NewsCountry("JP", "Japan", "ja"),
        NewsCountry("BR", "Brazil", "pt-BR"),
        NewsCountry("SG", "Singapore", "en-SG"),
        NewsCountry("ZA", "South Africa", "en-ZA"),
        NewsCountry("AE", "United Arab Emirates", "en-AE")
    )

    // Google News topic slugs used in /headlines/section/topic/{TOPIC}
    val categories = listOf(
        NewsCategory("TOP", "Top Stories", null),
        NewsCategory("WORLD", "World", "WORLD"),
        NewsCategory("NATION", "Nation", "NATION"),
        NewsCategory("BUSINESS", "Business", "BUSINESS"),
        NewsCategory("TECHNOLOGY", "Technology", "TECHNOLOGY"),
        NewsCategory("ENTERTAINMENT", "Entertainment", "ENTERTAINMENT"),
        NewsCategory("SPORTS", "Sports", "SPORTS"),
        NewsCategory("SCIENCE", "Science", "SCIENCE"),
        NewsCategory("HEALTH", "Health", "HEALTH")
    )
}
