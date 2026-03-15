package com.flexynotes.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.flexynotes.app.R
import com.flexynotes.data.UserPreferences
import com.flexynotes.util.DriveAuthManager
import com.flexynotes.util.SecureStorageManager
import com.flexynotes.worker.SyncManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudSyncScreen(
    preferences: UserPreferences,
    onUpdatePreferences: ((UserPreferences) -> UserPreferences) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    BackHandler { onNavigateBack() }
    val scrollState = rememberScrollState()
    var showWebDavDialog by remember { mutableStateOf(false) }

    // --- Global Encryption Password State ---
    val secureStorage = remember { SecureStorageManager(context) }
    var savedSyncPassword by remember { mutableStateOf(secureStorage.getSyncPassword() ?: "") }
    var passwordInput by remember { mutableStateOf(savedSyncPassword) }

    // Cloud settings are only enabled if a password is set
    val isCloudEnabled = savedSyncPassword.isNotBlank() && savedSyncPassword.length >= 4

    // Google Drive Setup
    val driveAuthManager = remember { DriveAuthManager(context) }
    var driveAccountEmail by remember { mutableStateOf(driveAuthManager.getSignedInAccount()?.email) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                driveAccountEmail = account?.email
                // Automatically enable the sync toggle when signed in successfully
                onUpdatePreferences { it.copy(isGoogleDriveSyncEnabled = true) }
                Toast.makeText(context, "Signed in as ${account?.email}", Toast.LENGTH_SHORT).show()
            } catch (e: ApiException) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud Sync Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // --- Step 1: Global Encryption Password ---
            Text(
                text = "1. Global Encryption",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Set a master password to encrypt your notes before uploading them to any cloud. You will need this password to restore notes on a new device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Encryption Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = passwordInput.isNotEmpty() && passwordInput.length < 4
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (passwordInput.length >= 4) {
                                secureStorage.saveSyncPassword(passwordInput)
                                savedSyncPassword = passwordInput
                                Toast.makeText(context, "Encryption password saved!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (savedSyncPassword.isEmpty()) "Save Password" else "Update Password")
                    }
                }
            }

            // --- Step 2: Background Sync (Requires Password) ---
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isCloudEnabled) 1f else 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                ListItem(
                    headlineContent = { Text("Master Auto-Sync", color = MaterialTheme.colorScheme.onPrimaryContainer) },
                    supportingContent = { Text("Enable background synchronization", color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)) },
                    trailingContent = {
                        Switch(
                            checked = preferences.isAutoSyncEnabled,
                            onCheckedChange = { isEnabled ->
                                onUpdatePreferences { it.copy(isAutoSyncEnabled = isEnabled) }
                                if (isEnabled) {
                                    SyncManager.schedulePeriodicSync(context)
                                    Toast.makeText(context, "Auto-Sync enabled", Toast.LENGTH_SHORT).show()
                                } else {
                                    SyncManager.cancelPeriodicSync(context)
                                    Toast.makeText(context, "Auto-Sync disabled", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = isCloudEnabled
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "2. Sync Destinations",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = if (isCloudEnabled) 1f else 0.5f),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
            )

            // WebDAV Destination
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("WebDAV Server", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCloudEnabled) 1f else 0.5f)) },
                        supportingContent = {
                            Text(
                                text = if (preferences.webDavUrl.isNotEmpty()) "Configured: ${preferences.webDavUrl}" else "Not configured",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCloudEnabled) 1f else 0.5f)
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = preferences.isWebDavSyncEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && preferences.webDavUrl.isEmpty()) {
                                        showWebDavDialog = true // Force setup if empty
                                    } else {
                                        onUpdatePreferences { it.copy(isWebDavSyncEnabled = isChecked) }
                                    }
                                },
                                enabled = isCloudEnabled
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    TextButton(
                        onClick = { showWebDavDialog = true },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        enabled = isCloudEnabled
                    ) {
                        Text("Configure WebDAV")
                    }
                }
            }

            // Google Drive Destination
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text("Google Drive", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isCloudEnabled) 1f else 0.5f)) },
                        supportingContent = {
                            Text(
                                text = driveAccountEmail ?: "Not signed in",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isCloudEnabled) 1f else 0.5f)
                            )
                        },
                        trailingContent = {
                            Checkbox(
                                checked = preferences.isGoogleDriveSyncEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked && driveAccountEmail == null) {
                                        signInLauncher.launch(driveAuthManager.getSignInIntent())
                                    } else {
                                        onUpdatePreferences { it.copy(isGoogleDriveSyncEnabled = isChecked) }
                                    }
                                },
                                enabled = isCloudEnabled
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    TextButton(
                        onClick = {
                            if (driveAccountEmail == null) {
                                signInLauncher.launch(driveAuthManager.getSignInIntent())
                            } else {
                                driveAuthManager.signOut {
                                    driveAccountEmail = null
                                    onUpdatePreferences { it.copy(isGoogleDriveSyncEnabled = false) }
                                    Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
                        enabled = isCloudEnabled
                    ) {
                        Text(if (driveAccountEmail == null) "Sign In" else "Sign Out")
                    }
                }
            }
        }

        // WebDAV Dialog (Removed Sync Password Field)
        if (showWebDavDialog) {
            WebDavConfigDialog(
                preferences = preferences,
                onSave = { newUrl, newUser, newPass ->
                    onUpdatePreferences {
                        it.copy(
                            webDavUrl = newUrl,
                            webDavUsername = newUser,
                            webDavPassword = newPass,
                            isWebDavSyncEnabled = true // Auto-enable on successful save
                        )
                    }
                },
                onDismiss = { showWebDavDialog = false }
            )
        }
    }
}

@Composable
fun WebDavConfigDialog(
    preferences: UserPreferences,
    onSave: (String, String, String) -> Unit, // REMOVED 4th parameter
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var url by remember { mutableStateOf(preferences.webDavUrl) }
    var username by remember { mutableStateOf(preferences.webDavUsername) }
    var password by remember { mutableStateOf(preferences.webDavPassword) }

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

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (url.isNotBlank() && username.isNotBlank() && password.isNotBlank()) {
                            isTesting = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val webDavManager = com.flexynotes.util.WebDavManager()
                                // Note: Testing with a dummy string, encryption isn't tested here anymore since it's global
                                val result = webDavManager.uploadBackup(
                                    serverUrl = url,
                                    username = username,
                                    appPassword = password,
                                    fileName = "flexynotes_test.txt",
                                    encryptedPayload = "Connection is working."
                                )

                                Dispatchers.Main.invoke {
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
                onSave(url, username, password) // Pass 3 parameters
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