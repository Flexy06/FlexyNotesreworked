package com.flexynotes.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flexynotes.data.NoteDao
import com.flexynotes.data.NoteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Migration from version 5 to 6 to change ID type to TEXT
    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notes_new` (
                    `id` TEXT NOT NULL, 
                    `title` TEXT NOT NULL, 
                    `content` TEXT NOT NULL, 
                    `imageUri` TEXT, 
                    `createdAt` INTEGER NOT NULL, 
                    `modifiedAt` INTEGER NOT NULL, 
                    `colorArgb` INTEGER, 
                    `isDeleted` INTEGER NOT NULL, 
                    `isArchived` INTEGER NOT NULL, 
                    `isChecklist` INTEGER NOT NULL, 
                    `reminderTime` INTEGER, 
                    PRIMARY KEY(`id`)
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO `notes_new` (`id`, `title`, `content`, `imageUri`, `createdAt`, `modifiedAt`, `colorArgb`, `isDeleted`, `isArchived`, `isChecklist`, `reminderTime`)
                SELECT CAST(`id` AS TEXT), `title`, `content`, `imageUri`, `createdAt`, `modifiedAt`, `colorArgb`, `isDeleted`, `isArchived`, `isChecklist`, `reminderTime` 
                FROM `notes`
                """.trimIndent()
            )

            database.execSQL("DROP TABLE `notes`")
            database.execSQL("ALTER TABLE `notes_new` RENAME TO `notes`")
        }
    }

    // NEW: Migration from version 6 to 7 to create the tombstones table
    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `tombstones` (`noteId` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`noteId`))"
            )
        }
    }

    @Provides
    @Singleton
    fun provideNoteDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java,
            "flexy_notes_db"
        )
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7) // Added the new migration here
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(database: NoteDatabase): NoteDao {
        return database.noteDao
    }
}