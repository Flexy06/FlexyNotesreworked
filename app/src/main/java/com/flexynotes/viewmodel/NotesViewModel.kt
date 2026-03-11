package com.flexynotes.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flexynotes.data.NoteEntity
import com.flexynotes.domain.usecase.*
import com.flexynotes.util.ReminderManager
import com.flexynotes.worker.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
}

@HiltViewModel
class NotesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getActiveNotesUseCase: GetActiveNotesUseCase,
    private val getArchivedNotesUseCase: GetArchivedNotesUseCase,
    private val getDeletedNotesUseCase: GetDeletedNotesUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val archiveNoteUseCase: ArchiveNoteUseCase,
    private val unarchiveNoteUseCase: UnarchiveNoteUseCase,
    private val moveToTrashUseCase: MoveToTrashUseCase,
    private val restoreNoteUseCase: RestoreNoteUseCase,
    private val deletePermanentlyUseCase: DeletePermanentlyUseCase,
    private val clearTrashUseCase: ClearTrashUseCase,
    private val reminderManager: ReminderManager
) : ViewModel() {

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    val activeNotes: StateFlow<List<NoteEntity>> = getActiveNotesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val archivedNotes: StateFlow<List<NoteEntity>> = getArchivedNotesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedNotes: StateFlow<List<NoteEntity>> = getDeletedNotesUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Changed ID type to String
    suspend fun getNoteById(id: String): NoteEntity? {
        return getNoteByIdUseCase(id)
    }

    fun addNote(title: String, content: String, isChecklist: Boolean = false, reminderTime: Long? = null) {
        viewModelScope.launch {
            val validReminderTime = if (reminderTime != null && reminderTime > System.currentTimeMillis()) reminderTime else null

            val newNote = NoteEntity(
                title = title,
                content = content,
                isChecklist = isChecklist,
                reminderTime = validReminderTime,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis()
            )

            try {
                addNoteUseCase(newNote)

                if (validReminderTime != null) {
                    // We extract the ID directly from the newly created NoteEntity
                    reminderManager.scheduleReminder(newNote.id, title, content, validReminderTime)
                }
                SyncManager.triggerImmediateUpload(context)
            } catch (e: IllegalArgumentException) {
                _uiEvent.send(UiEvent.ShowToast("Cannot save an empty note"))
            }
        }
    }

    fun updateNote(note: NoteEntity, newTitle: String, newContent: String, newReminderTime: Long? = null) {
        viewModelScope.launch {
            val isTimeValid = newReminderTime != null && newReminderTime > System.currentTimeMillis()
            val finalReminderTime = if (isTimeValid) newReminderTime else null

            val updatedNote = note.copy(
                title = newTitle,
                content = newContent,
                reminderTime = finalReminderTime,
                modifiedAt = System.currentTimeMillis()
            )

            updateNoteUseCase(updatedNote)

            if (finalReminderTime != null) {
                reminderManager.scheduleReminder(note.id, newTitle, newContent, finalReminderTime)
            } else if (note.reminderTime != null) {
                reminderManager.cancelReminder(note.id)
            }

            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch {
            archiveNoteUseCase(note)
            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch {
            unarchiveNoteUseCase(note)
            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun moveToTrash(note: NoteEntity) {
        viewModelScope.launch {
            moveToTrashUseCase(note)
            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch {
            restoreNoteUseCase(note)
            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            deletePermanentlyUseCase(note)
            SyncManager.triggerImmediateUpload(context)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            clearTrashUseCase()
            SyncManager.triggerImmediateUpload(context)
        }
    }
}