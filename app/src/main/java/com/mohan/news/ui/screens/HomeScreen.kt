package com.mohan.news.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mohan.news.FeedUiState
import com.mohan.news.data.Article
import com.mohan.news.tts.TtsState
import com.mohan.news.ui.components.ErrorStateView
import com.mohan.news.ui.components.InitialLoadingScreen
import com.mohan.news.ui.components.LiveFeedCard
import com.mohan.news.ui.components.NewsArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    feedState: FeedUiState,
    isRefreshing: Boolean,
    showRelatedCoverage: Boolean,
    ttsState: TtsState,
    onRefresh: () -> Unit,
    onOpenArticle: (title: String, link: String, sourceName: String) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleReadAloud: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("NEWS", style = MaterialTheme.typography.headlineSmall) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (feedState is FeedUiState.Loaded) {
                ExtendedFloatingActionButton(onClick = onToggleReadAloud) {
                    val icon = when (ttsState) {
                        TtsState.SPEAKING -> Icons.Filled.Pause
                        TtsState.PAUSED -> Icons.Filled.PlayArrow
                        TtsState.IDLE -> Icons.Filled.RecordVoiceOver
                    }
                    Icon(icon, contentDescription = "Read headlines aloud")
                    Spacer(Modifier.padding(4.dp))
                    Text(
                        when (ttsState) {
                            TtsState.SPEAKING -> "Pause"
                            TtsState.PAUSED -> "Resume"
                            TtsState.IDLE -> "Read aloud"
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (feedState) {
                is FeedUiState.Loading -> InitialLoadingScreen(modifier = Modifier.fillMaxSize())
                is FeedUiState.Error -> ErrorStateView(
                    message = feedState.message,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize()
                )
                is FeedUiState.Loaded -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                LiveFeedCard(
                                    article = feedState.topStory,
                                    onClick = {
                                        onOpenArticle(
                                            feedState.topStory.cleanTitle,
                                            feedState.topStory.link,
                                            feedState.topStory.sourceName
                                        )
                                    }
                                )
                            }
                            items(feedState.otherArticles, key = { it.link }) { article: Article ->
                                NewsArticleCard(
                                    article = article,
                                    showRelatedCoverage = showRelatedCoverage,
                                    onOpenArticle = onOpenArticle
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

