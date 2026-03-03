package com.example.FlexyNotes.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class SortOrder { DATE_EDITED, DATE_CREATED, ALPHABETICAL }

data class UserPreferences(
    val isOledMode: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val sortOrder: SortOrder = SortOrder.DATE_EDITED,
    val showTimestamp: Boolean = false,
    val useHaptics: Boolean = true,
    val isSecureMode: Boolean = false // New setting for privacy
)