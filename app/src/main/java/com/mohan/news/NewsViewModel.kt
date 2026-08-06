package com.mohan.news

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mohan.news.data.AppSettings
import com.mohan.news.data.AppThemeMode
import com.mohan.news.data.Article
import com.mohan.news.data.NewsCatalog
import com.mohan.news.data.SettingsRepository
import com.mohan.news.network.GoogleNewsUrlBuilder
import com.mohan.news.network.NewsRepository
import com.mohan.news.network.NewsResult
import com.mohan.news.tts.HeadlineTtsManager
import com.mohan.news.tts.TtsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class FeedUiState {
    object Loading : FeedUiState()
    data class Loaded(val topStory: Article, val otherArticles: List<Article>) : FeedUiState()
    data class Error(val message: String) : FeedUiState()
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val newsRepository = NewsRepository()
    val ttsManager = HeadlineTtsManager(application)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _settingsLoaded = MutableStateFlow(false)
    val settingsLoaded: StateFlow<Boolean> = _settingsLoaded.asStateFlow()

    private val _feedState = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val feedState: StateFlow<FeedUiState> = _feedState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _ttsState = MutableStateFlow(TtsState.IDLE)
    val ttsState: StateFlow<TtsState> = _ttsState.asStateFlow()

    private val _ttsCurrentIndex = MutableStateFlow(-1)
    val ttsCurrentIndex: StateFlow<Int> = _ttsCurrentIndex.asStateFlow()

    init {
        ttsManager.onStateChanged = { state -> _ttsState.value = state }
        ttsManager.onHeadlineIndexChanged = { idx -> _ttsCurrentIndex.value = idx }

        viewModelScope.launch {
            settingsRepository.settingsFlow.collectLatest { newSettings ->
                val categoryChanged = newSettings.categoryId != _settings.value.categoryId
                val countryChanged = newSettings.countryCode != _settings.value.countryCode
                val firstLoad = _settings.value == AppSettings() && _feedState.value is FeedUiState.Loading
                _settings.value = newSettings
                ttsManager.setSpeed(newSettings.ttsSpeed)
                ttsManager.setPitch(newSettings.ttsPitch)
                newSettings.ttsVoiceName?.let { ttsManager.setVoiceByName(it) }
                _settingsLoaded.value = true
                if (newSettings.hasCompletedOnboarding && (categoryChanged || countryChanged || firstLoad)) {
                    loadFeed()
                }
            }
        }
    }

    fun loadFeed(isPullToRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isPullToRefresh) _isRefreshing.value = true else _feedState.value = FeedUiState.Loading

            val s = _settings.value
            val category = NewsCatalog.categories.firstOrNull { it.id == s.categoryId }
            val url = GoogleNewsUrlBuilder.buildFeedUrl(s.countryCode, s.language, category?.topicParam)

            when (val result = newsRepository.fetchFeed(url)) {
                is NewsResult.Success -> {
                    val articles = result.articles
                    if (articles.isNotEmpty()) {
                        _feedState.value = FeedUiState.Loaded(
                            topStory = articles.first(),
                            otherArticles = articles.drop(1)
                        )
                    } else {
                        _feedState.value = FeedUiState.Error("No articles found")
                    }
                }
                is NewsResult.Error -> {
                    _feedState.value = FeedUiState.Error(result.message)
                }
            }
            _isRefreshing.value = false
        }
    }

    fun setThemeMode(mode: AppThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    fun setCountry(code: String, language: String) = viewModelScope.launch { settingsRepository.setCountry(code, language) }
    fun setCategory(categoryId: String) = viewModelScope.launch { settingsRepository.setCategory(categoryId) }
    fun setShowRelatedCoverage(enabled: Boolean) = viewModelScope.launch { settingsRepository.setShowRelatedCoverage(enabled) }
    fun setTtsSpeed(speed: Float) = viewModelScope.launch { settingsRepository.setTtsSpeed(speed) }
    fun setTtsPitch(pitch: Float) = viewModelScope.launch { settingsRepository.setTtsPitch(pitch) }
    fun setTtsVoice(name: String?) = viewModelScope.launch { settingsRepository.setTtsVoice(name) }
    fun completeOnboarding(countryCode: String, language: String, categoryId: String) {
        viewModelScope.launch {
            settingsRepository.setCountry(countryCode, language)
            settingsRepository.setCategory(categoryId)
            settingsRepository.setOnboardingComplete(true)
        }
    }

    fun availableVoiceNames(): List<String> = ttsManager.availableVoices().map { it.name }

    fun readHeadlinesAloud() {
        val state = _feedState.value
        if (state is FeedUiState.Loaded) {
            val headlines = (listOf(state.topStory) + state.otherArticles).map { it.cleanTitle }
            ttsManager.readHeadlines(headlines)
        }
    }

    fun pauseTts() = ttsManager.pause()
    fun resumeTts() = ttsManager.resume()
    fun stopTts() = ttsManager.stop()

    override fun onCleared() {
        super.onCleared()
        ttsManager.shutdown()
    }
}
