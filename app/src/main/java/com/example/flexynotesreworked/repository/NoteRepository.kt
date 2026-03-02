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
    // Stream of active notes (not in trash)
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    // Stream of deleted notes (trash bin)
    val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun upsertNote(note: NoteEntity) = noteDao.insertNote(note)

    // Moves note to trash bin by updating the isDeleted flag
    suspend fun moveNoteToTrash(note: NoteEntity) {
        noteDao.updateNote(note.copy(isDeleted = true, modifiedAt = System.currentTimeMillis()))
    }

    // Restores a note from the trash bin
    suspend fun restoreNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(isDeleted = false, modifiedAt = System.currentTimeMillis()))
    }

    // Permanently removes a note from the database
    suspend fun deletePermanently(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun clearTrash() = noteDao.clearTrash()
}