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

    val activeNotes: StateFlow<List<NoteEntity>> = repository.activeNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedNotes: StateFlow<List<NoteEntity>> = repository.archivedNotes
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

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.archiveNote(note)
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.unarchiveNote(note)
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