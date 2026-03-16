package com.flexynotes.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. Add TombstoneEntity to the entities array and bump the version
@Database(
    entities = [NoteEntity::class, TombstoneEntity::class],
    version = 7,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract val noteDao: NoteDao

    companion object {
        // 2. Define the migration to create the new table without losing old data
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tombstones` (" +
                            "`noteId` TEXT NOT NULL, " +
                            "`deletedAt` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`noteId`))"
                )
            }
        }
    }
}