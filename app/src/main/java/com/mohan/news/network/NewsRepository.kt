package com.mohan.news.network

import com.mohan.news.data.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class NewsResult {
    data class Success(val articles: List<Article>) : NewsResult()
    data class Error(val message: String) : NewsResult()
}

class NewsRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    suspend fun fetchFeed(url: String): NewsResult = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (Android) NewsApp").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext NewsResult.Error("Server returned ${response.code}")
                }
                val body = response.body?.string() ?: return@withContext NewsResult.Error("Empty response")
                val articles = RssParser.parse(body)
                if (articles.isEmpty()) {
                    NewsResult.Error("No articles found")
                } else {
                    NewsResult.Success(articles)
                }
            }
        } catch (e: IOException) {
            NewsResult.Error(e.message ?: "Network error. Check your connection.")
        } catch (e: Exception) {
            NewsResult.Error(e.message ?: "Failed to load news")
        }
    }
}
