package com.example.FlexyNotes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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
                title = { Text("Settings") },
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

            SettingsGroup(title = "Appearance") {
                ListPreference(
                    title = "Theme",
                    subtitle = when (preferences.themeMode) {
                        ThemeMode.SYSTEM -> "System default"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    options = mapOf(
                        ThemeMode.SYSTEM to "System default",
                        ThemeMode.LIGHT to "Light",
                        ThemeMode.DARK to "Dark"
                    ),
                    selectedValue = preferences.themeMode,
                    onValueSelected = { newTheme -> onUpdatePreferences { it.copy(themeMode = newTheme) } }
                )

                SwitchPreference(
                    title = "Dynamic Colors",
                    subtitle = "Matches app colors to your wallpaper",
                    checked = preferences.useDynamicColor,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(useDynamicColor = newVal) } }
                )

                SwitchPreference(
                    title = "Pure Black (OLED)",
                    subtitle = "Saves battery on OLED displays",
                    checked = preferences.isOledMode,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(isOledMode = newVal) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = "Behavior") {
                ListPreference(
                    title = "Default Sort Order",
                    subtitle = when (preferences.sortOrder) {
                        SortOrder.DATE_EDITED -> "Last edited"
                        SortOrder.DATE_CREATED -> "Creation date"
                        SortOrder.ALPHABETICAL -> "Alphabetical (A-Z)"
                    },
                    options = mapOf(
                        SortOrder.DATE_EDITED to "Last edited",
                        SortOrder.DATE_CREATED to "Creation date",
                        SortOrder.ALPHABETICAL to "Alphabetical (A-Z)"
                    ),
                    selectedValue = preferences.sortOrder,
                    onValueSelected = { newSort -> onUpdatePreferences { it.copy(sortOrder = newSort) } }
                )

                SwitchPreference(
                    title = "Show Timestamps",
                    subtitle = "Display dates on notes in the list",
                    checked = preferences.showTimestamp,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(showTimestamp = newVal) } }
                )

                SwitchPreference(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on swipes and actions",
                    checked = preferences.useHaptics,
                    onCheckedChange = { newVal -> onUpdatePreferences { it.copy(useHaptics = newVal) } }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SettingsGroup(title = "About") {
                ListItem(
                    headlineContent = { Text("Version") },
                    supportingContent = { Text("v0.5.1") },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Groups preferences in a rounded Material 3 surface
@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit
) {
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

// Standard Material 3 switch list item
@Composable
private fun SwitchPreference(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable { onCheckedChange(!checked) }
    )
}

// Opens an AlertDialog for single-choice selections
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
                            RadioButton(
                                selected = value == selectedValue,
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}