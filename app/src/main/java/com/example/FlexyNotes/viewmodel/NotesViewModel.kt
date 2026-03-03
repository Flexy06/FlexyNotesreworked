package com.example.FlexyNotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.FlexyNotes.data.NoteEntity
import com.example.FlexyNotes.data.SortOrder
import com.example.FlexyNotes.data.UserPreferencesRepository
import com.example.FlexyNotes.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    // Combines DB flows with preferences to sort on the fly
    val activeNotes: StateFlow<List<NoteEntity>> = combine(
        repository.activeNotes,
        userPreferencesRepository.userPreferencesFlow
    ) { notes, prefs ->
        sortNotes(notes, prefs.sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val archivedNotes: StateFlow<List<NoteEntity>> = combine(
        repository.archivedNotes,
        userPreferencesRepository.userPreferencesFlow
    ) { notes, prefs ->
        sortNotes(notes, prefs.sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val deletedNotes: StateFlow<List<NoteEntity>> = combine(
        repository.deletedNotes,
        userPreferencesRepository.userPreferencesFlow
    ) { notes, prefs ->
        sortNotes(notes, prefs.sortOrder)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun sortNotes(notes: List<NoteEntity>, sortOrder: SortOrder): List<NoteEntity> {
        return when (sortOrder) {
            SortOrder.DATE_EDITED -> notes.sortedByDescending { it.modifiedAt }
            SortOrder.DATE_CREATED -> notes.sortedByDescending { it.createdAt }
            SortOrder.ALPHABETICAL -> notes.sortedWith(
                compareBy<NoteEntity> {
                    // Push completely empty notes to the very bottom
                    it.title.isBlank() && it.content.isBlank()
                }.thenBy {
                    // Fallback to content if the title is empty
                    it.title.ifBlank { it.content }.lowercase()
                }
            )
        }
    }

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