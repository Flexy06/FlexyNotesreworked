package com.example.flexynotesreworked.repository

import com.example.flexynotesreworked.data.NoteDao
import com.example.flexynotesreworked.data.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun upsertNote(note: NoteEntity) = noteDao.insertNote(note)

    suspend fun moveNoteToTrash(note: NoteEntity) {
        noteDao.updateNote(note.copy(isDeleted = true, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun archiveNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(isArchived = true, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun unarchiveNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(isArchived = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun restoreNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(isDeleted = false, modifiedAt = System.currentTimeMillis()))
    }

    suspend fun deletePermanently(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun clearTrash() = noteDao.clearTrash()
}