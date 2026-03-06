package com.flexynotes.data

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class SortOrder { DATE_EDITED, DATE_CREATED, ALPHABETICAL }
enum class AppLanguage { SYSTEM, ENGLISH, GERMAN, FRENCH }

data class UserPreferences(
    val isOledMode: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val sortOrder: SortOrder = SortOrder.DATE_EDITED,
    val showTimestamp: Boolean = true,
    val useHaptics: Boolean = true,
    val isSecureMode: Boolean = false,
    val isAppLockEnabled: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val askForCrashReports: Boolean = true
)