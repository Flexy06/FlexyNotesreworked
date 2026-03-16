package com.flexynotes.domain.usecase

import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val getActiveNotesUseCase: GetActiveNotesUseCase,
    private val getArchivedNotesUseCase: GetArchivedNotesUseCase,
    private val getDeletedNotesUseCase: GetDeletedNotesUseCase,
    private val getTombstonesUseCase: GetTombstonesUseCase // NEW
) {
    // Fetches current notes and tombstones, converts to JSON, and encrypts
    suspend operator fun invoke(password: String): Result<String> {
        return try {
            val activeNotes = getActiveNotesUseCase().first()
            val archivedNotes = getArchivedNotesUseCase().first()
            val deletedNotes = getDeletedNotesUseCase().first()
            val tombstones = getTombstonesUseCase().first()

            val allNotesToBackup = activeNotes + archivedNotes + deletedNotes

            // We still allow backup if notes are empty but tombstones exist (to sync deletions)
            if (allNotesToBackup.isEmpty() && tombstones.isEmpty()) {
                return Result.failure(Exception("No data available to backup."))
            }

            val jsonString = BackupJsonManager.createJsonFromBackup(allNotesToBackup, tombstones)
            val encryptedData = EncryptionManager.encrypt(jsonString, password)

            Result.success(encryptedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}