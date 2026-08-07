package com.mohan.news.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "news_settings")

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val countryCode: String = "US",
    val language: String = "en-US",
    val categoryId: String = "TOP",
    val showRelatedCoverage: Boolean = true,
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsVoiceName: String? = null,
    val hasCompletedOnboarding: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val COUNTRY = stringPreferencesKey("country_code")
        val LANGUAGE = stringPreferencesKey("language")
        val CATEGORY = stringPreferencesKey("category_id")
        val SHOW_RELATED = booleanPreferencesKey("show_related_coverage")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        val TTS_VOICE = stringPreferencesKey("tts_voice_name")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("has_completed_onboarding")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            countryCode = prefs[Keys.COUNTRY] ?: "US",
            language = prefs[Keys.LANGUAGE] ?: "en-US",
            categoryId = prefs[Keys.CATEGORY] ?: "TOP",
            showRelatedCoverage = prefs[Keys.SHOW_RELATED] ?: true,
            ttsSpeed = prefs[Keys.TTS_SPEED] ?: 1.0f,
            ttsPitch = prefs[Keys.TTS_PITCH] ?: 1.0f,
            ttsVoiceName = prefs[Keys.TTS_VOICE],
            hasCompletedOnboarding = prefs[Keys.ONBOARDING_COMPLETE] ?: false
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setCountry(code: String, language: String) {
        context.dataStore.edit {
            it[Keys.COUNTRY] = code
            it[Keys.LANGUAGE] = language
        }
    }

    suspend fun setCategory(categoryId: String) {
        context.dataStore.edit { it[Keys.CATEGORY] = categoryId }
    }

    suspend fun setShowRelatedCoverage(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_RELATED] = enabled }
    }

    suspend fun setTtsSpeed(speed: Float) {
        context.dataStore.edit { it[Keys.TTS_SPEED] = speed }
    }

    suspend fun setTtsPitch(pitch: Float) {
        context.dataStore.edit { it[Keys.TTS_PITCH] = pitch }
    }

    suspend fun setTtsVoice(voiceName: String?) {
        context.dataStore.edit {
            if (voiceName == null) it.remove(Keys.TTS_VOICE) else it[Keys.TTS_VOICE] = voiceName
        }
    }

    suspend fun setOnboardingComplete(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETE] = completed }
    }

    suspend fun completeOnboarding(countryCode: String, language: String, categoryId: String) {
        context.dataStore.edit {
            it[Keys.COUNTRY] = countryCode
            it[Keys.LANGUAGE] = language
            it[Keys.CATEGORY] = categoryId
            it[Keys.ONBOARDING_COMPLETE] = true
        }
    }
}
