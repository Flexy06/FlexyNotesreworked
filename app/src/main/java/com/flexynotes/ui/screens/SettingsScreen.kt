package com.flexynotes.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.flexynotes.app.R
import com.flexynotes.data.AppLanguage
import com.flexynotes.data.SortOrder
import com.flexynotes.data.ThemeMode
import com.flexynotes.data.UserPreferences
import com.flexynotes.util.CrashReporter
//import androidx.lifecycle.compose.collectAsStateWithLifecycle -> handled by MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
     useHaptics: Boolean,
    onOpenDrawer: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

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
                        AppLanguage.FRENCH to "Français"
                    ),
                    selectedValue = preferences.language,
                    onValueSelected = { newLang ->
                        onUpdatePreferences { it.copy(language = newLang) }

                        val localeTag = when (newLang) {
                            AppLanguage.ENGLISH -> "en"
                            AppLanguage.GERMAN -> "de"
                            AppLanguage.FRENCH -> "fr"
                            AppLanguage.SYSTEM -> ""
                        }
                        val appLocale = if (localeTag.isEmpty()) {
                            LocaleListCompat.getEmptyLocaleList()
                        } else {
                            LocaleListCompat.forLanguageTags(localeTag)
                        }
                        AppCompatDelegate.setApplicationLocales(appLocale)
                    }
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

            SettingsGroup(title = stringResource(R.string.settings_advanced)) {
                SwitchPreference(
                    title = stringResource(R.string.settings_ask_crash_reports),
                    subtitle = stringResource(R.string.settings_ask_crash_reports_desc),
                    checked = preferences.askForCrashReports,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(askForCrashReports = newVal) } }
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_send_last_crash)) },
                    supportingContent = { Text(stringResource(R.string.settings_send_last_crash_desc)) },
                    modifier = Modifier.clickable {
                        val log = CrashReporter.getCrashLog(context)
                        if (log != null) {
                            val developerEmail = context.getString(R.string.developer_email)

                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:$developerEmail")
                                putExtra(Intent.EXTRA_SUBJECT, "FlexyNotes Manual Crash Report")
                                putExtra(Intent.EXTRA_TEXT, "Device: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}\n\nCrash Log:\n\n$log")
                            }
                            context.startActivity(emailIntent)
                        } else {
                            Toast.makeText(context, "No crash log found", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )


            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = stringResource(R.string.settings_about)) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    supportingContent = { Text("v1.1.2") },
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