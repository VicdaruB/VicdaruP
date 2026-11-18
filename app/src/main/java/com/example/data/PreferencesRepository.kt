package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AnimeSeries
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vicdaruDataStore: DataStore<Preferences> by preferencesDataStore(name = "vicdaru_prefs")

data class LocalNotificationPrefs(
    val darkMode: Boolean = true,
    val morningQuoteEnabled: Boolean = true,
    val morningHour: Int = 8,
    val morningMinute: Int = 0,
    val randomQuotesEnabled: Boolean = true,
    val activeThemeAnime: String = AnimeSeries.SAKURA.key,
    val lastQuoteText: String = "",
    val lastQuoteCharacter: String = "",
    val lastQuoteImageUrl: String = "",
    val lastQuoteAnime: String = AnimeSeries.SAKURA.key,
    val usedImageUrls: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false,
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val MORNING_ENABLED = booleanPreferencesKey("morning_enabled")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")
        val RANDOM_ENABLED = booleanPreferencesKey("random_enabled")
        val ACTIVE_ANIME = stringPreferencesKey("active_theme_anime")
        val LAST_QUOTE_TEXT = stringPreferencesKey("last_quote_text")
        val LAST_QUOTE_CHAR = stringPreferencesKey("last_quote_char")
        val LAST_QUOTE_IMG = stringPreferencesKey("last_quote_img")
        val LAST_QUOTE_ANIME = stringPreferencesKey("last_quote_anime")
        val USED_IMAGE_URLS = stringSetPreferencesKey("used_image_urls")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val prefsFlow: Flow<LocalNotificationPrefs> = context.vicdaruDataStore.data.map { prefs ->
        LocalNotificationPrefs(
            darkMode = prefs[Keys.DARK_MODE] ?: true,
            morningQuoteEnabled = prefs[Keys.MORNING_ENABLED] ?: true,
            morningHour = prefs[Keys.MORNING_HOUR] ?: 8,
            morningMinute = prefs[Keys.MORNING_MINUTE] ?: 0,
            randomQuotesEnabled = prefs[Keys.RANDOM_ENABLED] ?: true,
            activeThemeAnime = prefs[Keys.ACTIVE_ANIME] ?: AnimeSeries.SAKURA.key,
            lastQuoteText = prefs[Keys.LAST_QUOTE_TEXT] ?: "",
            lastQuoteCharacter = prefs[Keys.LAST_QUOTE_CHAR] ?: "",
            lastQuoteImageUrl = prefs[Keys.LAST_QUOTE_IMG] ?: "",
            lastQuoteAnime = prefs[Keys.LAST_QUOTE_ANIME] ?: AnimeSeries.SAKURA.key,
            usedImageUrls = prefs[Keys.USED_IMAGE_URLS] ?: emptySet(),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun getSnapshot(): LocalNotificationPrefs = prefsFlow.first()

    suspend fun saveSettingsMirror(
        darkMode: Boolean,
        morningEnabled: Boolean,
        morningHour: Int,
        morningMinute: Int,
        randomEnabled: Boolean,
        activeThemeAnime: String,
    ) {
        context.vicdaruDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = darkMode
            prefs[Keys.MORNING_ENABLED] = morningEnabled
            prefs[Keys.MORNING_HOUR] = morningHour
            prefs[Keys.MORNING_MINUTE] = morningMinute
            prefs[Keys.RANDOM_ENABLED] = randomEnabled
            prefs[Keys.ACTIVE_ANIME] = activeThemeAnime
        }
    }

    suspend fun saveLastDeliveredQuote(
        animeKey: String,
        quoteText: String,
        characterName: String,
        imageUrl: String,
    ) {
        context.vicdaruDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_ANIME] = animeKey
            prefs[Keys.LAST_QUOTE_ANIME] = animeKey
            prefs[Keys.LAST_QUOTE_TEXT] = quoteText
            prefs[Keys.LAST_QUOTE_CHAR] = characterName
            prefs[Keys.LAST_QUOTE_IMG] = imageUrl
            if (imageUrl.isNotBlank()) {
                val existing = prefs[Keys.USED_IMAGE_URLS]?.toMutableSet() ?: mutableSetOf()
                existing.add(imageUrl)
                // Keep bounded to last 500 URLs to avoid unbounded DataStore growth
                prefs[Keys.USED_IMAGE_URLS] = if (existing.size > 500) {
                    existing.toList().takeLast(500).toSet()
                } else {
                    existing
                }
            }
        }
    }

    suspend fun recordUsedImageUrl(imageUrl: String) {
        if (imageUrl.isBlank()) return
        context.vicdaruDataStore.edit { prefs ->
            val existing = prefs[Keys.USED_IMAGE_URLS]?.toMutableSet() ?: mutableSetOf()
            existing.add(imageUrl)
            prefs[Keys.USED_IMAGE_URLS] = if (existing.size > 500) {
                existing.toList().takeLast(500).toSet()
            } else {
                existing
            }
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.vicdaruDataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }
}
