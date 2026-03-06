package com.flexynotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexynotes.data.NoteEntity
import com.flexynotes.repository.NoteRepository
import com.flexynotes.util.ReminderManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository,
    private val reminderManager: ReminderManager // Make sure ReminderManager is injected
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

    fun addNote(title: String, content: String, isChecklist: Boolean = false, reminderTime: Long? = null) {
        viewModelScope.launch {
            // FIX: Don't set reminders that are in the past
            val validReminderTime = if (reminderTime != null && reminderTime > System.currentTimeMillis()) reminderTime else null

            val newNote = NoteEntity(
                title = title,
                content = content,
                isChecklist = isChecklist,
                reminderTime = validReminderTime,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )
            val id = repository.upsertNote(newNote)

            if (validReminderTime != null) {
                reminderManager.scheduleReminder(id, title, content, validReminderTime)
            }
        }
    }

    fun updateNote(note: NoteEntity, newTitle: String, newContent: String, newReminderTime: Long? = null) {
        viewModelScope.launch {
            // FIX: Automatically clear reminder if the user saves a note where the reminder is already in the past
            val isTimeValid = newReminderTime != null && newReminderTime > System.currentTimeMillis()
            val finalReminderTime = if (isTimeValid) newReminderTime else null

            val updatedNote = note.copy(
                title = newTitle,
                content = newContent,
                reminderTime = finalReminderTime,
                modifiedAt = System.currentTimeMillis()
            )
            repository.upsertNote(updatedNote)

            if (finalReminderTime != null) {
                reminderManager.scheduleReminder(note.id, newTitle, newContent, finalReminderTime)
            } else if (note.reminderTime != null) {
                // Cancel if it was removed or if it became invalid (past)
                reminderManager.cancelReminder(note.id)
            }
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch { repository.archiveNote(note) }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch { repository.unarchiveNote(note) }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch { repository.moveNoteToTrash(note) }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch { repository.restoreNote(note) }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch { repository.deletePermanently(note) }
    }

    fun clearTrash() {
        viewModelScope.launch { repository.clearTrash() }
    }
}