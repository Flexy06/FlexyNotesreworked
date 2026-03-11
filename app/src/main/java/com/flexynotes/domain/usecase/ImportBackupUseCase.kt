package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase
) {
    suspend operator fun invoke(encryptedData: String, password: String): Result<Int> {
        return try {
            val jsonString = EncryptionManager.decrypt(encryptedData, password)
            val backupNotes = BackupJsonManager.parseNotesFromJson(jsonString)

            var importedCount = 0

            for (backupNote in backupNotes) {
                val existingNote = getNoteByIdUseCase(backupNote.id)

                if (existingNote == null) {
                    // Note is completely new -> Insert it with all states
                    val newNote = NoteEntity(
                        id = backupNote.id,
                        title = backupNote.title,
                        content = backupNote.content,
                        isChecklist = backupNote.isChecklist,
                        reminderTime = backupNote.reminderTime,
                        createdAt = backupNote.createdAt,
                        modifiedAt = backupNote.modifiedAt,
                        // NEU: Status-Flags übernehmen
                        isArchived = backupNote.isArchived,
                        isDeleted = backupNote.isDeleted
                    )
                    addNoteUseCase(newNote)
                    importedCount++
                } else {
                    // Note exists -> Check which one is newer
                    if (backupNote.modifiedAt > existingNote.modifiedAt) {
                        // Cloud version is newer -> Update local note and its state
                        val updatedNote = existingNote.copy(
                            title = backupNote.title,
                            content = backupNote.content,
                            isChecklist = backupNote.isChecklist,
                            reminderTime = backupNote.reminderTime,
                            modifiedAt = backupNote.modifiedAt,
                            // NEU: Status-Flags überschreiben
                            isArchived = backupNote.isArchived,
                            isDeleted = backupNote.isDeleted
                        )
                        updateNoteUseCase(updatedNote)
                        importedCount++
                    }
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Decryption failed or corrupted file."))
        }
    }
}