package com.mohan.news

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mohan.news.tts.TtsState
import com.mohan.news.ui.screens.AboutScreen
import com.mohan.news.ui.screens.HomeScreen
import com.mohan.news.ui.screens.SettingsScreen
import com.mohan.news.ui.theme.NewsAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()

            NewsAppTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor
            ) {
                val navController = rememberNavController()
                val feedState by viewModel.feedState.collectAsState()
                val isRefreshing by viewModel.isRefreshing.collectAsState()
                val ttsState by viewModel.ttsState.collectAsState()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            feedState = feedState,
                            isRefreshing = isRefreshing,
                            showRelatedCoverage = settings.showRelatedCoverage,
                            ttsState = ttsState,
                            onRefresh = { viewModel.loadFeed(isPullToRefresh = true) },
                            onOpenLink = { url -> openInBrowser(url) },
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenAbout = { navController.navigate("about") },
                            onToggleReadAloud = {
                                when (ttsState) {
                                    TtsState.IDLE -> viewModel.readHeadlinesAloud()
                                    TtsState.SPEAKING -> viewModel.pauseTts()
                                    TtsState.PAUSED -> viewModel.resumeTts()
                                }
                            }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settings = settings,
                            availableVoiceNames = viewModel.availableVoiceNames(),
                            onBack = { navController.popBackStack() },
                            onThemeModeChange = viewModel::setThemeMode,
                            onDynamicColorChange = viewModel::setDynamicColor,
                            onCountryChange = viewModel::setCountry,
                            onCategoryChange = viewModel::setCategory,
                            onShowRelatedChange = viewModel::setShowRelatedCoverage,
                            onTtsSpeedChange = viewModel::setTtsSpeed,
                            onTtsPitchChange = viewModel::setTtsPitch,
                            onTtsVoiceChange = viewModel::setTtsVoice
                        )
                    }
                    composable("about") {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }

    private fun openInBrowser(url: String) {
        if (url.isBlank()) return
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopTts()
    }
}
