package com.flexynotes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.domain.usecase.ImportBackupUseCase
import com.flexynotes.util.DriveAuthManager
import com.flexynotes.util.GoogleDriveManager
import com.flexynotes.util.WebDavManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupUiEvent {
    data class ShowToast(val message: String) : BackupUiEvent()
    data class PayloadReadyForExport(val payload: String) : BackupUiEvent()
    object CloudActionStarted : BackupUiEvent()
    object CloudActionFinished : BackupUiEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {

    private val _uiEvent = Channel<BackupUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val webDavManager = WebDavManager()
    private val webDavFileName = "flexynotes_cloud_backup.json"
    private val driveFileName = "flexynotes_drive_backup.json"

    // Generates the encrypted string and sends it to the UI for local saving
    fun prepareExport(password: String) {
        viewModelScope.launch {
            val result = exportBackupUseCase(password)
            result.onSuccess { payload ->
                _uiEvent.send(BackupUiEvent.PayloadReadyForExport(payload))
            }
            result.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast(it.message ?: "Export failed"))
            }
        }
    }

    // Takes the encrypted string from a local file and decrypts it
    fun processImport(encryptedPayload: String, password: String) {
        viewModelScope.launch {
            val result = importBackupUseCase(encryptedPayload, password)
            result.onSuccess { count ->
                _uiEvent.send(BackupUiEvent.ShowToast("Successfully imported $count notes!"))
            }
            result.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast("Import failed. Wrong password?"))
            }
        }
    }

    // --- WebDAV Actions ---

    fun uploadToWebDav(password: String, url: String, user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val exportResult = exportBackupUseCase(password)
            exportResult.onSuccess { payload ->
                val uploadResult = webDavManager.uploadBackup(url, user, pass, webDavFileName, payload)
                if (uploadResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("WebDAV backup successful!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("WebDAV upload failed: ${uploadResult.exceptionOrNull()?.message}"))
                }
            }
            exportResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast(it.message ?: "Encryption failed"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }

    fun downloadFromWebDav(password: String, url: String, user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val downloadResult = webDavManager.downloadBackup(url, user, pass, webDavFileName)
            downloadResult.onSuccess { payload ->
                val importResult = importBackupUseCase(payload, password)
                if (importResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("Restored ${importResult.getOrNull()} notes from WebDAV!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("Import failed. Wrong password?"))
                }
            }
            downloadResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast("WebDAV download failed: ${it.message}"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }

    // --- Google Drive Actions ---

    fun uploadToGoogleDrive(context: Context, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val account = DriveAuthManager(context).getSignedInAccount()
            if (account == null) {
                _uiEvent.send(BackupUiEvent.ShowToast("Google Drive not signed in"))
                _uiEvent.send(BackupUiEvent.CloudActionFinished)
                return@launch
            }

            val exportResult = exportBackupUseCase(password)
            exportResult.onSuccess { payload ->
                val driveManager = GoogleDriveManager(context, account)
                val uploadResult = driveManager.uploadBackup(driveFileName, payload)

                if (uploadResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("Google Drive backup successful!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("Google Drive upload failed: ${uploadResult.exceptionOrNull()?.message}"))
                }
            }
            exportResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast(it.message ?: "Encryption failed"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }

    fun downloadFromGoogleDrive(context: Context, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val account = DriveAuthManager(context).getSignedInAccount()
            if (account == null) {
                _uiEvent.send(BackupUiEvent.ShowToast("Google Drive not signed in"))
                _uiEvent.send(BackupUiEvent.CloudActionFinished)
                return@launch
            }

            val driveManager = GoogleDriveManager(context, account)
            val downloadResult = driveManager.downloadBackup(driveFileName)

            downloadResult.onSuccess { payload ->
                val importResult = importBackupUseCase(payload, password)
                if (importResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("Restored ${importResult.getOrNull()} notes from Google Drive!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("Import failed. Wrong password?"))
                }
            }
            downloadResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast("Google Drive download failed: ${it.message}"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }
}