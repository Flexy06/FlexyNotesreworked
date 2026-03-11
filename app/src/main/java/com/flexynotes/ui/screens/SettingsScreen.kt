package com.flexynotes.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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
import androidx.core.os.LocaleListCompat
import com.flexynotes.app.R
import com.flexynotes.data.AppLanguage
import com.flexynotes.data.SortOrder
import com.flexynotes.data.ThemeMode
import com.flexynotes.data.UserPreferences
import com.flexynotes.util.CrashReporter
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.OutlinedTextField
import com.flexynotes.ui.BackupDialog
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import kotlinx.coroutines.launch
import kotlinx.coroutines.invoke
import com.flexynotes.worker.SyncManager // <-- NEU: Import für den SyncManager

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
    var showBackupDialog by remember { mutableStateOf(false) }
    var showWebDavDialog by remember { mutableStateOf(false) }

    // Fetch the app version dynamically from the package manager
    val appVersion = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

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
        Box(modifier = Modifier.fillMaxSize()) {
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

                SettingsGroup(title = "Backup & Restore") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBackupDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Encrypted Local Backup", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text("Export or import notes securely", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Default.Lock, contentDescription = "Backup", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                // Cloud Sync Group
                SettingsGroup(title = "Cloud Sync (WebDAV)") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showWebDavDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Configure WebDAV", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                text = if (preferences.webDavUrl.isNotEmpty()) "Configured" else "Not configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.CloudUpload, contentDescription = "WebDAV Setup", tint = MaterialTheme.colorScheme.primary)
                    }

                    if (preferences.webDavUrl.isNotEmpty()) {
                        SwitchPreference(
                            title = "Enable Auto-Sync",
                            subtitle = "Sync notes automatically in the background",
                            checked = preferences.isAutoSyncEnabled,
                            onCheckedChange = { isEnabled ->
                                onUpdatePreferences { it.copy(isAutoSyncEnabled = isEnabled) }

                                if (isEnabled) {
                                    SyncManager.schedulePeriodicSync(context)
                                    SyncManager.triggerImmediateDownload(context)
                                    Toast.makeText(context, "Auto-Sync enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    SyncManager.cancelPeriodicSync(context)
                                    Toast.makeText(context, "Auto-Sync disabled", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
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
                        supportingContent = { Text("v.$appVersion") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            if (showBackupDialog) {
                BackupDialog(
                    preferences = preferences,
                    onDismiss = { showBackupDialog = false }
                )
            }

            if (showWebDavDialog) {
                WebDavConfigDialog(
                    preferences = preferences,
                    // NEU: 4 Parameter statt 3
                    onSave = { newUrl, newUser, newPass, newSyncPass ->
                        onUpdatePreferences {
                            it.copy(
                                webDavUrl = newUrl,
                                webDavUsername = newUser,
                                webDavPassword = newPass
                            )
                        }
                        // NEU: Das Verschlüsselungspasswort direkt im SecureStorage speichern
                        val secureStorage = com.flexynotes.util.SecureStorageManager(context)
                        secureStorage.saveSyncPassword(newSyncPass)
                    },
                    onDismiss = { showWebDavDialog = false }
                )
            }
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

@Composable
fun WebDavConfigDialog(
    preferences: UserPreferences,
    // NEU: Die Funktion erwartet jetzt 4 Strings
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Den SecureStorage laden, um ein evtl. schon gespeichertes Passwort anzuzeigen
    val secureStorage = remember { com.flexynotes.util.SecureStorageManager(context) }

    var url by remember { mutableStateOf(preferences.webDavUrl) }
    var username by remember { mutableStateOf(preferences.webDavUsername) }
    var password by remember { mutableStateOf(preferences.webDavPassword) }
    // NEU: State für das Sync-Passwort
    var syncPassword by remember { mutableStateOf(secureStorage.getSyncPassword() ?: "") }

    var isTesting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cloud Sync (WebDAV)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Server URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("App Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // NEU: Eingabefeld für das Verschlüsselungspasswort
                OutlinedTextField(
                    value = syncPassword,
                    onValueChange = { syncPassword = it },
                    label = { Text("Encryption Password") },
                    supportingText = { Text("Used to encrypt your notes before upload") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // NEU: Prüfen, ob auch das 4. Feld ausgefüllt ist
                        if (url.isNotBlank() && username.isNotBlank() && password.isNotBlank() && syncPassword.isNotBlank()) {
                            isTesting = true
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                val webDavManager = com.flexynotes.util.WebDavManager()
                                val result = webDavManager.uploadBackup(
                                    serverUrl = url,
                                    username = username,
                                    appPassword = password,
                                    fileName = "flexynotes_test.txt",
                                    encryptedPayload = "Hello from FlexyNotes! Connection is working."
                                )

                                kotlinx.coroutines.Dispatchers.Main.invoke {
                                    isTesting = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Connection successful!", Toast.LENGTH_LONG).show()
                                    } else {
                                        val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                                        Toast.makeText(context, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please fill in all fields first", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Test Connection")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // NEU: Alle 4 Werte an onSave übergeben
                onSave(url, username, password, syncPassword)
                onDismiss()
            }) {
                Text(stringResource(R.string.save, "Save"))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}