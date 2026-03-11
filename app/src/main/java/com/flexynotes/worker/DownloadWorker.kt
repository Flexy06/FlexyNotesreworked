package com.flexynotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flexynotes.data.UserPreferencesRepository
import com.flexynotes.domain.usecase.ImportBackupUseCase
import com.flexynotes.util.SecureStorageManager
import com.flexynotes.util.WebDavManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val importBackupUseCase: ImportBackupUseCase,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = preferencesRepository.userPreferencesFlow.first()
        val secureStorage = SecureStorageManager(applicationContext)
        val webDavManager = WebDavManager()

        val url = preferences.webDavUrl
        val username = preferences.webDavUsername
        val appPassword = preferences.webDavPassword
        val syncPassword = secureStorage.getSyncPassword()

        // Abort if cloud sync is not configured or no master password is set
        if (url.isBlank() || username.isBlank() || appPassword.isBlank() || syncPassword.isNullOrBlank()) {
            return Result.success()
        }

        return try {
            // Fetch the encrypted backup file from the WebDAV server
            val downloadResult = webDavManager.downloadBackup(
                serverUrl = url,
                username = username,
                appPassword = appPassword,
                fileName = "flexynotes_cloud_backup.json"
            )

            if (downloadResult.isSuccess) {
                val encryptedPayload = downloadResult.getOrThrow()

                // Intelligently merge the downloaded notes with the local database
                val importResult = importBackupUseCase(encryptedPayload, syncPassword)

                if (importResult.isSuccess) {
                    Result.success()
                } else {
                    Result.failure()
                }
            } else {
                // Retry later if the download fails (e.g., no internet connection)
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}