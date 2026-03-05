package com.example.FlexyNotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.FlexyNotes.R
import com.example.FlexyNotes.data.AppLanguage
import com.example.FlexyNotes.data.SortOrder
import com.example.FlexyNotes.data.ThemeMode
import com.example.FlexyNotes.data.UserPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsGroup(title = stringResource(R.string.settings_appearance)) {
                ListPreference(
                    title = stringResource(R.string.settings_language),
                    subtitle = when (preferences.language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguage.ENGLISH -> "English"
                        AppLanguage.GERMAN -> "Deutsch"
                        AppLanguage.FRENCH -> "Français"
                    },
                    options = mapOf(
                        AppLanguage.SYSTEM to stringResource(R.string.settings_language_system),
                        AppLanguage.ENGLISH to "English",
                        AppLanguage.GERMAN to "Deutsch",
                        AppLanguage.FRENCH to "Français",

                    ),
                    selectedValue = preferences.language,
                    onValueSelected = { newLang -> onUpdatePreferences { it.copy(language = newLang) } }
                )

                ListPreference(
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (preferences.themeMode) {
                        ThemeMode.SYSTEM -> stringResource(R.string.settings_language_system)
                        ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                        ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                    },
                    options = mapOf(
                        ThemeMode.SYSTEM to stringResource(R.string.settings_language_system),
                        ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                        ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                    ),
                    selectedValue = preferences.themeMode,
                    onValueSelected = { newTheme -> onUpdatePreferences { it.copy(themeMode = newTheme) } }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_dynamic_colors),
                    subtitle = stringResource(R.string.settings_dynamic_colors_desc),
                    checked = preferences.useDynamicColor,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(useDynamicColor = newVal) } }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_oled),
                    subtitle = stringResource(R.string.settings_oled_desc),
                    checked = preferences.isOledMode,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(isOledMode = newVal) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.settings_behavior)) {
                ListPreference(
                    title = stringResource(R.string.settings_sort),
                    subtitle = when (preferences.sortOrder) {
                        SortOrder.DATE_EDITED -> stringResource(R.string.settings_sort_edited)
                        SortOrder.DATE_CREATED -> stringResource(R.string.settings_sort_created)
                        SortOrder.ALPHABETICAL -> stringResource(R.string.settings_sort_alpha)
                    },
                    options = mapOf(
                        SortOrder.DATE_EDITED to stringResource(R.string.settings_sort_edited),
                        SortOrder.DATE_CREATED to stringResource(R.string.settings_sort_created),
                        SortOrder.ALPHABETICAL to stringResource(R.string.settings_sort_alpha)
                    ),
                    selectedValue = preferences.sortOrder,
                    onValueSelected = { newSort -> onUpdatePreferences { it.copy(sortOrder = newSort) } }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_timestamps),
                    subtitle = stringResource(R.string.settings_timestamps_desc),
                    checked = preferences.showTimestamp,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(showTimestamp = newVal) } }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_desc),
                    checked = preferences.useHaptics,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(useHaptics = newVal) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.settings_privacy)) {
                SwitchPreference(
                    title = stringResource(R.string.settings_app_lock),
                    subtitle = stringResource(R.string.settings_app_lock_desc),
                    checked = preferences.isAppLockEnabled,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(isAppLockEnabled = newVal) } }
                )

                SwitchPreference(
                    title = stringResource(R.string.settings_secure_mode),
                    subtitle = stringResource(R.string.settings_secure_mode_desc),
                    checked = preferences.isSecureMode,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(isSecureMode = newVal) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.settings_about)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    supportingContent = { Text("v0.9.6") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SwitchPreference(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onCheckedChange) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

@Composable
private fun <T> ListPreference(
    title: String,
    subtitle: String,
    options: Map<T, String>,
    selectedValue: T,
    onValueSelected: (T) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(title) },
            text = {
                Column {
                    options.forEach { (value, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onValueSelected(value)
                                    showDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            RadioButton(selected = (value == selectedValue), onClick = null)
                            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}