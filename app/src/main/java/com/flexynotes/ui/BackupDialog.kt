package com.flexynotes.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flexynotes.data.UserPreferences
import com.flexynotes.util.DriveAuthManager
import com.flexynotes.util.FileHelper
import com.flexynotes.viewmodel.BackupUiEvent
import com.flexynotes.viewmodel.BackupViewModel

@Composable
fun BackupDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var pendingPayload by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Check WebDAV configuration
    val hasWebDavConfig = preferences.webDavUrl.isNotBlank() &&
            preferences.webDavUsername.isNotBlank() &&
            preferences.webDavPassword.isNotBlank()

    // Check Google Drive configuration
    val driveAuthManager = remember { DriveAuthManager(context) }
    val googleAccount = remember { driveAuthManager.getSignedInAccount() }
    val hasGoogleDriveConfig = googleAccount != null

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingPayload != null) {
            val success = FileHelper.writeTextToUri(context, uri, pendingPayload!!)
            if (success) Toast.makeText(context, "Local backup saved!", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "Error saving file.", Toast.LENGTH_SHORT).show()
        }
        pendingPayload = null
        onDismiss()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val payload = FileHelper.readTextFromUri(context, uri)
            if (payload != null && password.isNotBlank()) {
                viewModel.processImport(payload, password)
            } else {
                Toast.makeText(context, "Cannot read file or empty password.", Toast.LENGTH_SHORT).show()
            }
        }
        onDismiss()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BackupUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is BackupUiEvent.PayloadReadyForExport -> {
                    pendingPayload = event.payload
                    exportLauncher.launch("flexynotes_backup_${System.currentTimeMillis()}.json")
                }
                is BackupUiEvent.CloudActionStarted -> isLoading = true
                is BackupUiEvent.CloudActionFinished -> {
                    isLoading = false
                    onDismiss()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text("Manual Backup & Restore") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Enter your master password. You will need this to restore your notes later!", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Encryption Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = password.isNotEmpty() && password.length < 4,
                    supportingText = { if (password.isNotEmpty() && password.length < 4) Text("Password must be at least 4 characters") }
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Local Actions ---
                    Text("Local Device", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(
                            onClick = { viewModel.prepareExport(password) },
                            enabled = password.length >= 4
                        ) { Text("Save") }

                        TextButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                            enabled = password.length >= 4
                        ) { Text("Load") }
                    }

                    // --- WebDAV Actions ---
                    if (hasWebDavConfig) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("WebDAV Server", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = { viewModel.uploadToWebDav(password, preferences.webDavUrl, preferences.webDavUsername, preferences.webDavPassword) },
                                enabled = password.length >= 4
                            ) { Text("Backup") }

                            TextButton(
                                onClick = { viewModel.downloadFromWebDav(password, preferences.webDavUrl, preferences.webDavUsername, preferences.webDavPassword) },
                                enabled = password.length >= 4
                            ) { Text("Restore") }
                        }
                    }

                    // --- Google Drive Actions ---
                    if (hasGoogleDriveConfig) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Google Drive", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Button(
                                onClick = { viewModel.uploadToGoogleDrive(context, password) },
                                enabled = password.length >= 4
                            ) { Text("Backup") }

                            TextButton(
                                onClick = { viewModel.downloadFromGoogleDrive(context, password) },
                                enabled = password.length >= 4
                            ) { Text("Restore") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!isLoading) onDismiss() }) {
                Text("Close")
            }
        }
    )
}