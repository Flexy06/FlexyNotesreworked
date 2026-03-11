package com.flexynotes.data

import androidx.room.Database
import androidx.room.RoomDatabase

// Defines the database configuration and serves as the main access point
@Database(
    entities = [NoteEntity::class],
    version = 6,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    // Exposes the Data Access Object (DAO) for the notes
    abstract val noteDao: NoteDao
}