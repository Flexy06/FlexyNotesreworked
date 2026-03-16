package com.flexynotes.domain.usecase

import com.flexynotes.data.NoteEntity
import com.flexynotes.data.TombstoneEntity
import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ImportBackupUseCase @Inject constructor(
    private val addNoteUseCase: AddNoteUseCase,
    private val updateNoteUseCase: UpdateNoteUseCase,
    private val getNoteByIdUseCase: GetNoteByIdUseCase,
    private val deletePermanentlyUseCase: DeletePermanentlyUseCase,
    private val getTombstonesUseCase: GetTombstonesUseCase,
    private val insertTombstoneUseCase: InsertTombstoneUseCase
) {
    suspend operator fun invoke(encryptedData: String, password: String): Result<Int> {
        return try {
            val jsonString = EncryptionManager.decrypt(encryptedData, password)
            val container = BackupJsonManager.parseContainerFromJson(jsonString)
                ?: return Result.failure(Exception("Corrupted backup file."))

            var importedCount = 0

            // 1. Fetch local tombstones to prevent resurrecting notes we already deleted locally
            val localTombstones = getTombstonesUseCase().first().associateBy { it.noteId }

            // 2. Process Cloud Tombstones: Delete local notes if the cloud says they are dead
            for (cloudTombstone in container.tombstones) {
                val existingNote = getNoteByIdUseCase(cloudTombstone.noteId)

                if (existingNote != null && cloudTombstone.deletedAt >= existingNote.modifiedAt) {
                    // Cloud deletion is newer than our local edit -> kill it permanently
                    deletePermanentlyUseCase(existingNote)
                } else if (existingNote == null && !localTombstones.containsKey(cloudTombstone.noteId)) {
                    // We don't have the note, but we also don't have the tombstone. Save it locally
                    // to keep the history in sync across devices.
                    insertTombstoneUseCase(TombstoneEntity(cloudTombstone.noteId, cloudTombstone.deletedAt))
                }
            }

            // 3. Process Cloud Notes: Insert or update local notes
            for (backupNote in container.notes) {
                val localTombstone = localTombstones[backupNote.id]

                // ANTI-ZOMBIE CHECK: Did we delete this note locally AFTER it was last modified in the cloud?
                if (localTombstone != null && localTombstone.deletedAt >= backupNote.modifiedAt) {
                    continue // Skip this note, keep it dead
                }

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
                        modifiedAt = backupNote.modifiedAt,
                        isArchived = backupNote.isArchived,
                        isDeleted = backupNote.isDeleted,
                        colorArgb = backupNote.colorArgb,
                        imageUri = backupNote.imageUri
                    )
                    addNoteUseCase(newNote)
                    importedCount++
                } else {
                    // Note exists -> Update only if cloud version is newer
                    if (backupNote.modifiedAt > existingNote.modifiedAt) {
                        val updatedNote = existingNote.copy(
                            title = backupNote.title,
                            content = backupNote.content,
                            isChecklist = backupNote.isChecklist,
                            reminderTime = backupNote.reminderTime,
                            modifiedAt = backupNote.modifiedAt,
                            isArchived = backupNote.isArchived,
                            isDeleted = backupNote.isDeleted,
                            colorArgb = backupNote.colorArgb,
                            imageUri = backupNote.imageUri
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