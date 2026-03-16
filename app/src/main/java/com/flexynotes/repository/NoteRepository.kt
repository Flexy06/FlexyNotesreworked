package com.flexynotes.repository

import com.flexynotes.data.NoteDao
import com.flexynotes.data.NoteEntity
import com.flexynotes.data.TombstoneEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    val activeNotes: Flow<List<NoteEntity>> = noteDao.getActiveNotes()
    val archivedNotes: Flow<List<NoteEntity>> = noteDao.getArchivedNotes()
    val deletedNotes: Flow<List<NoteEntity>> = noteDao.getDeletedNotes()

    // Expose tombstones for the backup export
    val tombstones: Flow<List<TombstoneEntity>> = noteDao.getAllTombstones()

    suspend fun getNoteById(id: String): NoteEntity? = noteDao.getNoteById(id)

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

    suspend fun insertTombstone(tombstone: TombstoneEntity) {
        noteDao.insertTombstone(tombstone)
    }


    suspend fun deletePermanently(note: NoteEntity) {
        // Create a tombstone before removing the note completely to track the deletion for cloud sync
        val tombstone = TombstoneEntity(
            noteId = note.id,
            deletedAt = System.currentTimeMillis()
        )
        noteDao.insertTombstone(tombstone)
        noteDao.deleteNote(note)
    }

    suspend fun clearTrash() {
        // Fetch all notes currently in the trash to create tombstones for them before clearing
        val notesToDelete = deletedNotes.first()

        notesToDelete.forEach { note ->
            val tombstone = TombstoneEntity(
                noteId = note.id,
                deletedAt = System.currentTimeMillis()
            )
            noteDao.insertTombstone(tombstone)
        }

        noteDao.clearTrash()
    }

    // Calculates the most recent modification time across all notes and tombstones
    suspend fun getLatestLocalModificationTime(): Long {
        val latestNote = noteDao.getLatestNoteTimestamp() ?: 0L
        val latestTombstone = noteDao.getLatestTombstoneTimestamp() ?: 0L
        return maxOf(latestNote, latestTombstone)
    }


}