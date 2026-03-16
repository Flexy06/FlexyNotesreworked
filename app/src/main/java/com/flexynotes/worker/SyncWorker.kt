package com.flexynotes.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flexynotes.data.UserPreferencesRepository
import com.flexynotes.domain.usecase.ExportBackupUseCase
import com.flexynotes.domain.usecase.ImportBackupUseCase
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
    private val importBackupUseCase: ImportBackupUseCase,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Two-Way SyncWorker started")

        val preferences = preferencesRepository.userPreferencesFlow.first()
        val secureStorage = SecureStorageManager(applicationContext)
        val syncPassword = secureStorage.getSyncPassword()

        if (syncPassword.isNullOrBlank()) {
            Log.w("SyncWorker", "No sync password set, aborting sync")
            return Result.success()
        }

        val url = preferences.webDavUrl
        val username = preferences.webDavUsername
        val appPassword = preferences.webDavPassword
        val isWebDavActive = preferences.isWebDavSyncEnabled && url.isNotBlank() && username.isNotBlank() && appPassword.isNotBlank()

        val driveAuthManager = DriveAuthManager(applicationContext)
        val googleAccount = driveAuthManager.getSignedInAccount()
        val isGoogleDriveActive = preferences.isGoogleDriveSyncEnabled && googleAccount != null

        if (!isWebDavActive && !isGoogleDriveActive) {
            return Result.success()
        }

        return try {
            var webDavSuccess = true
            var driveSuccess = true

            // STEP 1: Download and merge (Import) from active clouds
            if (isWebDavActive) {
                val webDavManager = WebDavManager()
                val downloadResult = webDavManager.downloadBackup(url, username, appPassword, "flexynotes_cloud_backup.json")
                if (downloadResult.isSuccess) {
                    importBackupUseCase(downloadResult.getOrThrow(), syncPassword)
                }
            }

            if (isGoogleDriveActive) {
                val driveManager = GoogleDriveManager(applicationContext, googleAccount!!)
                val downloadResult = driveManager.downloadBackup("flexynotes_drive_backup.json")
                if (downloadResult.isSuccess) {
                    importBackupUseCase(downloadResult.getOrThrow(), syncPassword)
                }
            }

            // STEP 2: Export the newly merged local database
            val exportResult = exportBackupUseCase(syncPassword)
            if (exportResult.isFailure) {
                return Result.failure()
            }
            val mergedPayload = exportResult.getOrThrow()

            // STEP 3: Upload the merged state back to the clouds
            if (isWebDavActive) {
                val webDavManager = WebDavManager()
                val uploadResult = webDavManager.uploadBackup(url, username, appPassword, "flexynotes_cloud_backup.json", mergedPayload)
                webDavSuccess = uploadResult.isSuccess
                if (!webDavSuccess) Log.e("SyncWorker", "WebDAV upload failed")
            }

            if (isGoogleDriveActive) {
                val driveManager = GoogleDriveManager(applicationContext, googleAccount!!)
                val uploadResult = driveManager.uploadBackup("flexynotes_drive_backup.json", mergedPayload)
                driveSuccess = uploadResult.isSuccess
                if (!driveSuccess) Log.e("SyncWorker", "Google Drive upload failed")
            }

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