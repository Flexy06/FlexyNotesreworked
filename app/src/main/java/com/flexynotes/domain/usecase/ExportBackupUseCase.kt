package com.flexynotes.domain.usecase

import com.flexynotes.util.BackupJsonManager
import com.flexynotes.util.EncryptionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportBackupUseCase @Inject constructor(
    private val getActiveNotesUseCase: GetActiveNotesUseCase,
    private val getArchivedNotesUseCase: GetArchivedNotesUseCase
) {
    // Fetches current notes, converts to JSON, and encrypts with the provided password
    suspend operator fun invoke(password: String): Result<String> {
        return try {
            // Collect the latest list from the flows
            val activeNotes = getActiveNotesUseCase().first()
            val archivedNotes = getArchivedNotesUseCase().first()

            val allNotesToBackup = activeNotes + archivedNotes

            if (allNotesToBackup.isEmpty()) {
                return Result.failure(Exception("No notes available to backup."))
            }

            val jsonString = BackupJsonManager.createJsonFromNotes(allNotesToBackup)
            val encryptedData = EncryptionManager.encrypt(jsonString, password)

            Result.success(encryptedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}