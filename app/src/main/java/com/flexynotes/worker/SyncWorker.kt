package com.flexynotes.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flexynotes.data.UserPreferencesRepository
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.util.DriveAuthManager
import com.flexynotes.util.GoogleDriveManager
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
        Log.d("SyncWorker", "SyncWorker started successfully")

        val preferences = preferencesRepository.userPreferencesFlow.first()
        val secureStorage = SecureStorageManager(applicationContext)
        val syncPassword = secureStorage.getSyncPassword()

        // Abort if no encryption password is set
        if (syncPassword.isNullOrBlank()) {
            Log.w("SyncWorker", "No sync password set, aborting sync")
            return Result.success()
        }

        // Check WebDAV configuration AND user toggle
        val url = preferences.webDavUrl
        val username = preferences.webDavUsername
        val appPassword = preferences.webDavPassword
        val isWebDavActive = preferences.isWebDavSyncEnabled && url.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()

        // Check Google Drive configuration AND user toggle
        val driveAuthManager = DriveAuthManager(applicationContext)
        val googleAccount = driveAuthManager.getSignedInAccount()
        val isGoogleDriveActive = preferences.isGoogleDriveSyncEnabled && googleAccount != null

        // Abort if no cloud sync methods are actively enabled by the user
        if (!isWebDavActive && !isGoogleDriveActive) {
            return Result.success()
        }

        return try {
            val exportResult = exportBackupUseCase(syncPassword)

            if (exportResult.isFailure) {
                return Result.failure()
            }

            val payload = exportResult.getOrThrow()
            var webDavSuccess = true
            var driveSuccess = true

            // Handle WebDAV Upload
            if (isWebDavActive) {
                val webDavManager = WebDavManager()
                val uploadResult = webDavManager.uploadBackup(
                    serverUrl = url,
                    username = username,
                    appPassword = appPassword,
                    fileName = "flexynotes_cloud_backup.json",
                    encryptedPayload = payload
                )
                webDavSuccess = uploadResult.isSuccess
                if (!webDavSuccess) Log.e("SyncWorker", "WebDAV upload failed")
            }

            // Handle Google Drive Upload
            if (isGoogleDriveActive) {
                val driveManager = GoogleDriveManager(applicationContext, googleAccount!!)
                val driveResult = driveManager.uploadBackup(
                    fileName = "flexynotes_drive_backup.json",
                    fileContent = payload
                )
                driveSuccess = driveResult.isSuccess
                if (!driveSuccess) {
                    Log.e("SyncWorker", "Google Drive upload failed: ${driveResult.exceptionOrNull()?.message}")
                }
            }

            // Retry if any ACTIVE upload failed (e.g., due to network issues)
            if ((isWebDavActive && !webDavSuccess) || (isGoogleDriveActive && !driveSuccess)) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Log.e("SyncWorker", "Unexpected error during sync", e)
            Result.retry()
        }
    }
}