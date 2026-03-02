package com.example.FlexyNotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FlexyNotes.data.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Liest den aktuellen OLED-Status aus dem DataStore und wandelt ihn in einen StateFlow um
    val isOledMode: StateFlow<Boolean> = userPreferencesRepository.isOledMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Standardwert, bis der DataStore geladen ist
        )

    // Speichert den neuen Wert dauerhaft im DataStore
    fun updateOledMode(isOled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateOledMode(isOled)
        }
    }
}