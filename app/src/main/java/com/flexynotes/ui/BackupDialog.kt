package com.flexynotes.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.flexynotes.util.FileHelper
import com.flexynotes.viewmodel.BackupUiEvent
import com.flexynotes.viewmodel.BackupViewModel

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var password by remember { mutableStateOf("") }

    // State to hold payload temporarily until the file picker returns a URI
    var pendingPayload by remember { mutableStateOf<String?>(null) }

    // 1. Launcher for EXPORT (Creates a new file)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingPayload != null) {
            val success = FileHelper.writeTextToUri(context, uri, pendingPayload!!)
            if (success) Toast.makeText(context, "Backup saved!", Toast.LENGTH_SHORT).show()
            else Toast.makeText(context, "Error saving file.", Toast.LENGTH_SHORT).show()
        }
        pendingPayload = null // Clear memory
        onDismiss()
    }

    // 2. Launcher for IMPORT (Opens an existing file)
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

    // Listen to ViewModel events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is BackupUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is BackupUiEvent.PayloadReadyForExport -> {
                    pendingPayload = event.payload
                    // Suggest a default filename
                    exportLauncher.launch("flexynotes_backup_${System.currentTimeMillis()}.json")
                }
            }
        }
    }

    // Simple Dialog UI
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypted Backup") },
        text = {
            Column {
                Text("Enter a master password. You will need this password to restore your notes later!")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.prepareExport(password) },
                enabled = password.length >= 4
            ) { Text("Export") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                enabled = password.length >= 4
            ) { Text("Import") }
        }
    )
}