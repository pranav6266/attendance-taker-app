package com.pranav.attendencetaker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pranav.attendencetaker.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("app_theme")
    private val NOTIF_MORNING_KEY = booleanPreferencesKey("notif_morning")
    private val NOTIF_EVENING_KEY = booleanPreferencesKey("notif_evening")

    // --- THEME ---
    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        val themeName = prefs[THEME_KEY] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: Exception) {
            AppTheme.SYSTEM
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }

    // --- NOTIFICATIONS ---
    val morningReminderFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIF_MORNING_KEY] ?: true // Default true
    }

    val eveningReminderFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[NOTIF_EVENING_KEY] ?: true // Default true
    }

    suspend fun setMorningReminder(enabled: Boolean) {
        context.dataStore.edit { it[NOTIF_MORNING_KEY] = enabled }
    }

    suspend fun setEveningReminder(enabled: Boolean) {
        context.dataStore.edit { it[NOTIF_EVENING_KEY] = enabled }
    }
}