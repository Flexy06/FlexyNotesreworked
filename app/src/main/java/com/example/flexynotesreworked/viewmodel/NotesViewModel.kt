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

    val notes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedNotes: StateFlow<List<NoteEntity>> = repository.deletedNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Fetches a single note for the editor
    suspend fun getNoteById(id: Long): NoteEntity? {
        return repository.getNoteById(id)
    }

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

    // Updates an existing note while preserving its ID and creation date
    fun updateNote(note: NoteEntity, newTitle: String, newContent: String) {
        viewModelScope.launch {
            val updatedNote = note.copy(
                title = newTitle,
                content = newContent,
                modifiedAt = System.currentTimeMillis()
            )
            repository.upsertNote(updatedNote)
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