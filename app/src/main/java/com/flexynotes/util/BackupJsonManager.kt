package com.flexynotes.util

import com.flexynotes.data.NoteEntity
import com.flexynotes.data.backup.BackupContainer
import com.flexynotes.data.backup.BackupNote
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupJsonManager {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Converts database entities to a formatted JSON string
    fun createJsonFromNotes(notes: List<NoteEntity>): String {
        val backupNotes = notes.map { entity ->
            BackupNote(
                title = entity.title,
                content = entity.content,
                isChecklist = entity.isChecklist,
                reminderTime = entity.reminderTime,
                createdAt = entity.createdAt,
                modifiedAt = entity.modifiedAt
            )
        }

        val container = BackupContainer(
            version = 1,
            timestamp = System.currentTimeMillis(),
            notes = backupNotes
        )

        return jsonConfig.encodeToString(container)
    }

    // Parses a JSON string back into backup models
    fun parseNotesFromJson(jsonString: String): List<BackupNote> {
        return try {
            val container = jsonConfig.decodeFromString<BackupContainer>(jsonString)
            container.notes
        } catch (e: Exception) {
            emptyList()
        }
    }
}