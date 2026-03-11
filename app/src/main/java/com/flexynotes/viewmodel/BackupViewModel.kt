package com.flexynotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.domain.usecase.ImportBackupUseCase
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
    private val backupFileName = "flexynotes_cloud_backup.json"

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

    // Generates the encrypted string and uploads it directly to WebDAV
    fun uploadToCloud(password: String, url: String, user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val exportResult = exportBackupUseCase(password)
            exportResult.onSuccess { payload ->
                val uploadResult = webDavManager.uploadBackup(url, user, pass, backupFileName, payload)
                if (uploadResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("Cloud backup successful!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("Cloud upload failed: ${uploadResult.exceptionOrNull()?.message}"))
                }
            }
            exportResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast(it.message ?: "Encryption failed"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }

    // Downloads the encrypted string from WebDAV and decrypts it
    fun downloadFromCloud(password: String, url: String, user: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiEvent.send(BackupUiEvent.CloudActionStarted)

            val downloadResult = webDavManager.downloadBackup(url, user, pass, backupFileName)
            downloadResult.onSuccess { payload ->
                val importResult = importBackupUseCase(payload, password)
                if (importResult.isSuccess) {
                    _uiEvent.send(BackupUiEvent.ShowToast("Restored ${importResult.getOrNull()} notes from Cloud!"))
                } else {
                    _uiEvent.send(BackupUiEvent.ShowToast("Import failed. Wrong password?"))
                }
            }
            downloadResult.onFailure {
                _uiEvent.send(BackupUiEvent.ShowToast("Cloud download failed: ${it.message}"))
            }

            _uiEvent.send(BackupUiEvent.CloudActionFinished)
        }
    }
}