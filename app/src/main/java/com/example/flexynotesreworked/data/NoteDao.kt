package com.example.flexynotesreworked.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Only fetch notes that are NOT in the trash
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY modifiedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    // Fetch only deleted notes for the trash bin screen
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY modifiedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    suspend fun getNoteById(noteId: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    // Hard delete from database
    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Empty the trash bin
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearTrash()
}