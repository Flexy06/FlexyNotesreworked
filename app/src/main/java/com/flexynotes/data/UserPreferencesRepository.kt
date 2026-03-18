package com.flexynotes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
private val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")

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
        val IS_APP_LOCK_ENABLED = booleanPreferencesKey("is_app_lock_enabled")
        val LANGUAGE = stringPreferencesKey("language")
        val ASK_FOR_CRASH_REPORTS = booleanPreferencesKey("ask_for_crash_reports")

        val WEBDAV_URL = stringPreferencesKey("webdav_url")
        val WEBDAV_USERNAME = stringPreferencesKey("webdav_username")
        val WEBDAV_PASSWORD = stringPreferencesKey("webdav_password")

        val IS_WEBDAV_SYNC_ENABLED = booleanPreferencesKey("is_webdav_sync_enabled")
        val IS_GOOGLE_DRIVE_SYNC_ENABLED = booleanPreferencesKey("is_google_drive_sync_enabled")


        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")


    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isOledMode = preferences[IS_OLED_MODE] ?: false,
                themeMode = ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name),
                useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true,
                sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.DATE_EDITED.name),
                showTimestamp = preferences[SHOW_TIMESTAMP] ?: false,
                useHaptics = preferences[USE_HAPTICS] ?: true,
                isSecureMode = preferences[IS_SECURE_MODE] ?: false,
                isAppLockEnabled = preferences[IS_APP_LOCK_ENABLED] ?: false,
                language = AppLanguage.valueOf(preferences[LANGUAGE] ?: AppLanguage.SYSTEM.name),
                askForCrashReports = preferences[ASK_FOR_CRASH_REPORTS] ?: true,

                // Read WebDAV credentials
                webDavUrl = preferences[WEBDAV_URL] ?: "",
                webDavUsername = preferences[WEBDAV_USERNAME] ?: "",
                webDavPassword = preferences[WEBDAV_PASSWORD] ?: "",
                isAutoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: false,

                isWebDavSyncEnabled = preferences[IS_WEBDAV_SYNC_ENABLED] ?: false,
                isGoogleDriveSyncEnabled = preferences[IS_GOOGLE_DRIVE_SYNC_ENABLED] ?: false,

                // Read the last sync timestamp
                lastSyncTimestamp = preferences[LAST_SYNC_TIMESTAMP] ?: 0L
            )
        }

    suspend fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        context.dataStore.edit { preferences ->
            val current = UserPreferences(
                isOledMode = preferences[IS_OLED_MODE] ?: false,
                themeMode = ThemeMode.valueOf(preferences[THEME_MODE] ?: ThemeMode.SYSTEM.name),
                useDynamicColor = preferences[USE_DYNAMIC_COLOR] ?: true,
                sortOrder = SortOrder.valueOf(preferences[SORT_ORDER] ?: SortOrder.DATE_EDITED.name),
                showTimestamp = preferences[SHOW_TIMESTAMP] ?: false,
                useHaptics = preferences[USE_HAPTICS] ?: true,
                isSecureMode = preferences[IS_SECURE_MODE] ?: false,
                isAppLockEnabled = preferences[IS_APP_LOCK_ENABLED] ?: false,
                language = AppLanguage.valueOf(preferences[LANGUAGE] ?: AppLanguage.SYSTEM.name),
                askForCrashReports = preferences[ASK_FOR_CRASH_REPORTS] ?: true,

                // Map current WebDAV credentials
                webDavUrl = preferences[WEBDAV_URL] ?: "",
                webDavUsername = preferences[WEBDAV_USERNAME] ?: "",
                webDavPassword = preferences[WEBDAV_PASSWORD] ?: "",
                isAutoSyncEnabled = preferences[AUTO_SYNC_ENABLED] ?: false,

                isWebDavSyncEnabled = preferences[IS_WEBDAV_SYNC_ENABLED] ?: false,
                isGoogleDriveSyncEnabled = preferences[IS_GOOGLE_DRIVE_SYNC_ENABLED] ?: false,

                // Map the last sync timestamp
                lastSyncTimestamp = preferences[LAST_SYNC_TIMESTAMP] ?: 0L
            )
            val updated = update(current)

            preferences[IS_OLED_MODE] = updated.isOledMode
            preferences[THEME_MODE] = updated.themeMode.name
            preferences[USE_DYNAMIC_COLOR] = updated.useDynamicColor
            preferences[SORT_ORDER] = updated.sortOrder.name
            preferences[SHOW_TIMESTAMP] = updated.showTimestamp
            preferences[USE_HAPTICS] = updated.useHaptics
            preferences[IS_SECURE_MODE] = updated.isSecureMode
            preferences[IS_APP_LOCK_ENABLED] = updated.isAppLockEnabled
            preferences[LANGUAGE] = updated.language.name
            preferences[ASK_FOR_CRASH_REPORTS] = updated.askForCrashReports

            // Write WebDAV credentials
            preferences[WEBDAV_URL] = updated.webDavUrl
            preferences[WEBDAV_USERNAME] = updated.webDavUsername
            preferences[WEBDAV_PASSWORD] = updated.webDavPassword
            preferences[AUTO_SYNC_ENABLED] = updated.isAutoSyncEnabled

            preferences[IS_WEBDAV_SYNC_ENABLED] = updated.isWebDavSyncEnabled
            preferences[IS_GOOGLE_DRIVE_SYNC_ENABLED] = updated.isGoogleDriveSyncEnabled

            // Write the last sync timestamp
            preferences[LAST_SYNC_TIMESTAMP] = updated.lastSyncTimestamp
        }
    }

    // Updates the timestamp of the last successful synchronization
    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        updatePreferences { currentPreferences ->
            currentPreferences.copy(lastSyncTimestamp = timestamp)
        }
    }
}

