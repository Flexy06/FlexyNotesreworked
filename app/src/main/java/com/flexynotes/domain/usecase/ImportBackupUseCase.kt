package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase
) {
    // Decrypts payload, parses JSON, and inserts notes into the database
    suspend operator fun invoke(encryptedData: String, password: String): Result<Int> {
        return try {
            val jsonString = EncryptionManager.decrypt(encryptedData, password)
            val backupNotes = BackupJsonManager.parseNotesFromJson(jsonString)

            var importedCount = 0

            for (backupNote in backupNotes) {
                val newNote = NoteEntity(
                    title = backupNote.title,
                    content = backupNote.content,
                    isChecklist = backupNote.isChecklist,
                    reminderTime = backupNote.reminderTime,
                    createdAt = backupNote.createdAt,
                    modifiedAt = backupNote.modifiedAt
                )

                try {
                    addNoteUseCase(newNote)
                    importedCount++
                } catch (e: IllegalArgumentException) {
                    // Skip invalid or empty notes from the backup silently -> user will be notified
                    continue
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            // Fails if decryption fails (e.g., wrong password) or JSON is corrupted
            Result.failure(Exception("Decryption failed. Wrong password or corrupted file."))
        }
    }
}