package com.flexynotes.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.flexynotes.data.UserPreferences
import com.flexynotes.util.FileHelper
import com.flexynotes.viewmodel.BackupUiEvent
import com.flexynotes.viewmodel.BackupViewModel

@Composable
fun BackupDialog(
    preferences: UserPreferences, // Added preferences to get WebDAV credentials
    onDismiss: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }
    var pendingPayload by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val hasCloudConfig = preferences.webDavUrl.isNotBlank() &&
            preferences.webDavUsername.isNotBlank() &&
            preferences.webDavPassword.isNotBlank()

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
        title = { Text("Encrypted Backup") },
        text = {
            Column {
                Text("Enter your master password. You will need this to restore your notes later!")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Cloud Action Buttons
                if (hasCloudConfig) {
                    Button(
                        onClick = {
                            viewModel.uploadToCloud(password, preferences.webDavUrl, preferences.webDavUsername, preferences.webDavPassword)
                        },
                        enabled = password.length >= 4 && !isLoading
                    ) { Text("Backup to Cloud") }

                    TextButton(
                        onClick = {
                            viewModel.downloadFromCloud(password, preferences.webDavUrl, preferences.webDavUsername, preferences.webDavPassword)
                        },
                        enabled = password.length >= 4 && !isLoading
                    ) { Text("Restore from Cloud") }
                } else {
                    Text("Configure WebDAV in settings for Cloud Sync", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.Start) {
                // Local Action Buttons
                OutlinedButton(
                    onClick = { viewModel.prepareExport(password) },
                    enabled = password.length >= 4 && !isLoading
                ) { Text("Save Locally") }

                TextButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    enabled = password.length >= 4 && !isLoading
                ) { Text("Load Locally") }
            }
        }
    )
}