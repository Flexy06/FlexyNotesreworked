package com.flexynotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.domain.usecase.ImportBackupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BackupUiEvent {
    data class ShowToast(val message: String) : BackupUiEvent()
    data class PayloadReadyForExport(val payload: String) : BackupUiEvent()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase
) : ViewModel() {

    private val _uiEvent = Channel<BackupUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    // Generates the encrypted string and sends it to the UI for saving
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

    // Takes the encrypted string from the file and decrypts it
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
}