package com.flexynotes.util

import com.flexynotes.data.NoteEntity
import com.flexynotes.data.TombstoneEntity
import com.flexynotes.data.backup.BackupContainer
import com.flexynotes.data.backup.BackupNote
import com.flexynotes.data.backup.BackupTombstone
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object BackupJsonManager {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // Converts database entities and tombstones to a formatted JSON string
    fun createJsonFromBackup(notes: List<NoteEntity>, tombstones: List<TombstoneEntity>): String {
        val backupNotes = notes.map { entity ->
            BackupNote(
                id = entity.id,
                title = entity.title,
                content = entity.content,
                imageUri = entity.imageUri,
                createdAt = entity.createdAt,
                modifiedAt = entity.modifiedAt,
                colorArgb = entity.colorArgb,
                isDeleted = entity.isDeleted,
                isArchived = entity.isArchived,
                isChecklist = entity.isChecklist,
                reminderTime = entity.reminderTime
            )
        }

        val backupTombstones = tombstones.map { entity ->
            BackupTombstone(
                noteId = entity.noteId,
                deletedAt = entity.deletedAt
            )
        }

        val container = BackupContainer(
            version = 2, // Bumped version to 2 to indicate two-way sync support
            timestamp = System.currentTimeMillis(),
            notes = backupNotes,
            tombstones = backupTombstones
        )

        return jsonConfig.encodeToString(container)
    }

    // Parses a JSON string back into the full BackupContainer
    fun parseContainerFromJson(jsonString: String): BackupContainer? {
        return try {
            jsonConfig.decodeFromString<BackupContainer>(jsonString)
        } catch (e: Exception) {
            null
        }
    }
}