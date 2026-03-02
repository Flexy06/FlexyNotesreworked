package com.example.flexynotesreworked.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.flexynotesreworked.data.NoteEntity
import com.example.flexynotesreworked.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    // Converts the cold Flow from repository into a hot StateFlow for the UI
    // The flow remains active for 5 seconds after the last subscriber leaves
    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Separate stream for notes marked as deleted
    val deletedNotes: StateFlow<List<NoteEntity>> = repository.deletedNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val newNote = NoteEntity(
                title = title,
                content = content,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            repository.upsertNote(newNote)
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            repository.moveNoteToTrash(note)
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.restoreNote(note)
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            repository.deletePermanently(note)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }
}