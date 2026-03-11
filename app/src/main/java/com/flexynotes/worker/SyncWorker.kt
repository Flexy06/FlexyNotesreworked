package com.flexynotes.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flexynotes.data.UserPreferencesRepository
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.util.SecureStorageManager
import com.flexynotes.util.WebDavManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val exportBackupUseCase: ExportBackupUseCase,
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
            val exportResult = exportBackupUseCase(syncPassword)

            if (exportResult.isSuccess) {
                val payload = exportResult.getOrThrow()
                val uploadResult = webDavManager.uploadBackup(
                    serverUrl = url,
                    username = username,
                    appPassword = appPassword,
                    fileName = "flexynotes_cloud_backup.json",
                    encryptedPayload = payload
                )

                if (uploadResult.isSuccess) {
                    Result.success()
                } else {
                    // Retry later if upload fails (e.g., no internet)
                    Result.retry()
                }
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}