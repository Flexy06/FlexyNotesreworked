package com.flexynotes.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object SyncManager {
    private const val SYNC_WORK_NAME = "flexynotes_periodic_sync"
    private const val IMMEDIATE_UPLOAD_WORK_NAME = "flexynotes_immediate_upload"
    private const val IMMEDIATE_DOWNLOAD_WORK_NAME = "flexynotes_immediate_download"

    // 1. Periodic sync (Every 12 hours)
    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    // 2. Trigger immediate upload (Call this when a note is saved/changed)
    fun triggerImmediateUpload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val uploadRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_UPLOAD_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            uploadRequest
        )
    }

    // 3. Trigger immediate download (Call this on app open)
    fun triggerImmediateDownload(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Note: We need a separate DownloadWorker for this, or tell the SyncWorker to download first.
        // For now, we assume you create a DownloadWorker similar to your SyncWorker.
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_DOWNLOAD_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )
    }
}