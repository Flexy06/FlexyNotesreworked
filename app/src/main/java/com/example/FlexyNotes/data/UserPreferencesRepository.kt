package com.example.FlexyNotes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val IS_OLED_MODE = booleanPreferencesKey("is_oled_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val SHOW_TIMESTAMP = booleanPreferencesKey("show_timestamp")
        val USE_HAPTICS = booleanPreferencesKey("use_haptics")
        val IS_SECURE_MODE = booleanPreferencesKey("is_secure_mode")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isOledMode = preferences[IS_OLED_MODE] ?: true,
                themeMode = ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name),
                useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true,
                sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.DATE_EDITED.name),
                showTimestamp = preferences[SHOW_TIMESTAMP] ?: false,
                useHaptics = preferences[USE_HAPTICS] ?: true,
                isSecureMode = preferences[IS_SECURE_MODE] ?: false
            )
        }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { preferences ->
            val current = UserPreferences(
                isOledMode = preferences[IS_OLED_MODE] ?: true,
                themeMode = ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name),
                useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true,
                sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.DATE_EDITED.name),
                showTimestamp = preferences[SHOW_TIMESTAMP] ?: false,
                useHaptics = preferences[USE_HAPTICS] ?: true,
                isSecureMode = preferences[IS_SECURE_MODE] ?: false
            )
            val updated = update(current)

            preferences[IS_OLED_MODE] = updated.isOledMode
            preferences[THEME_MODE] = updated.themeMode.name
            preferences[USE_DYNAMIC_COLOR] = updated.useDynamicColor
            preferences[SORT_ORDER] = updated.sortOrder.name
            preferences[SHOW_TIMESTAMP] = updated.showTimestamp
            preferences[USE_HAPTICS] = updated.useHaptics
            preferences[IS_SECURE_MODE] = updated.isSecureMode
        }
    }
}