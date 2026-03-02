package com.example.FlexyNotes.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property to create a single instance of DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private companion object {
        val IS_OLED_MODE = booleanPreferencesKey("is_oled_mode")
    }

    // Exposes the current OLED mode setting as a Flow, defaults to true
    val isOledMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_OLED_MODE] ?: true
        }

    // Updates the OLED mode setting in DataStore
    suspend fun updateOledMode(isOled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_OLED_MODE] = isOled
        }
    }
}