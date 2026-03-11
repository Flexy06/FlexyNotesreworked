package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase // You need to inject this now!
) {
    // Decrypts payload, parses JSON, and intelligently merges notes based on modifiedAt timestamp
    suspend operator fun invoke(encryptedData: String, password: String): Result<Int> {
        return try {
            val jsonString = EncryptionManager.decrypt(encryptedData, password)
            val backupNotes = BackupJsonManager.parseNotesFromJson(jsonString)

            var importedCount = 0

            for (backupNote in backupNotes) {
                // Check if the note already exists locally
                val existingNote = getNoteByIdUseCase(backupNote.id)

                if (existingNote == null) {
                    // Note is completely new -> Insert it
                    val newNote = NoteEntity(
                        id = backupNote.id,
                        title = backupNote.title,
                        content = backupNote.content,
                        isChecklist = backupNote.isChecklist,
                        reminderTime = backupNote.reminderTime,
                        createdAt = backupNote.createdAt,
                        modifiedAt = backupNote.modifiedAt
                    )
                    addNoteUseCase(newNote)
                    importedCount++
                } else {
                    // Note exists -> Check which one is newer
                    if (backupNote.modifiedAt > existingNote.modifiedAt) {
                        // Cloud version is newer -> Update local note
                        val updatedNote = existingNote.copy(
                            title = backupNote.title,
                            content = backupNote.content,
                            isChecklist = backupNote.isChecklist,
                            reminderTime = backupNote.reminderTime,
                            modifiedAt = backupNote.modifiedAt
                        )
                        updateNoteUseCase(updatedNote)
                        importedCount++
                    }
                    // If local is newer or same age, we do nothing (keep local)
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Decryption failed or corrupted file."))
        }
    }
}