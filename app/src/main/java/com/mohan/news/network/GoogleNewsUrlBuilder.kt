package com.mohan.news.network

import android.net.Uri
import com.mohan.news.data.NewsCatalog

/**
 * Builds Google News RSS feed URLs.
 *
 * Top headlines for a country:
 *   https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en
 *
 * Topic-based category feed:
 *   https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en
 *
 * Search feed:
 *   https://news.google.com/rss/search?q=QUERY&hl=en-US&gl=US&ceid=US:en
 */
object GoogleNewsUrlBuilder {

    fun buildFeedUrl(countryCode: String, language: String, categoryTopic: String?): String {
        val ceidLang = language.substringBefore("-").ifBlank { "en" }
        val base = if (categoryTopic == null) {
            "https://news.google.com/rss"
        } else {
            "https://news.google.com/rss/headlines/section/topic/$categoryTopic"
        }
        return Uri.parse(base).buildUpon()
            .appendQueryParameter("hl", language)
            .appendQueryParameter("gl", countryCode)
            .appendQueryParameter("ceid", "$countryCode:$ceidLang")
            .build()
            .toString()
    }

    fun buildSearchUrl(query: String, countryCode: String, language: String): String {
        val ceidLang = language.substringBefore("-").ifBlank { "en" }
        return Uri.parse("https://news.google.com/rss/search").buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("hl", language)
            .appendQueryParameter("gl", countryCode)
            .appendQueryParameter("ceid", "$countryCode:$ceidLang")
            .build()
            .toString()
    }

    fun defaultCountry() = NewsCatalog.countries.first { it.code == "US" }
    fun defaultCategory() = NewsCatalog.categories.first { it.id == "TOP" }
}
