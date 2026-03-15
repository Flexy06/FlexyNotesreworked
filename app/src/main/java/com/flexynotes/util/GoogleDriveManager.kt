package com.flexynotes.util

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleDriveManager(context: Context, account: GoogleSignInAccount) {

    private val driveService: Drive

    init {
        // Initializes the credential with the required AppData scope
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        // Builds the Drive API client instance
        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("FlexyNotes")
            .build()
    }

    // Uploads or overwrites a file in the hidden AppData folder
    suspend fun uploadBackup(fileName: String, fileContent: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Defines the file type as JSON
                val mediaContent = ByteArrayContent.fromString("application/json", fileContent)
                val existingFileId = getFileId(fileName)

                val uploadedFile = if (existingFileId != null) {
                    // Update existing file: Must NOT include the 'parents' field
                    val updateMetadata = com.google.api.services.drive.model.File().apply {
                        name = fileName
                    }
                    driveService.files().update(existingFileId, updateMetadata, mediaContent).execute()
                } else {
                    // Create new file: MUST include the 'parents' field
                    val createMetadata = com.google.api.services.drive.model.File().apply {
                        name = fileName
                        parents = listOf("appDataFolder")
                    }
                    driveService.files().create(createMetadata, mediaContent).execute()
                }

                Result.success(uploadedFile.id)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    // Downloads a file from the hidden AppData folder
    suspend fun downloadBackup(fileName: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val fileId = getFileId(fileName)

                if (fileId == null) {
                    return@withContext Result.failure(Exception("Backup file not found in Google Drive."))
                }

                val outputStream = java.io.ByteArrayOutputStream()
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                val fileContent = outputStream.toString("UTF-8")
                Result.success(fileContent)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Searches for an existing file in the AppData folder to prevent duplicates
    private fun getFileId(fileName: String): String? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$fileName'")
            .setFields("files(id, name)")
            .execute()

        return result.files?.firstOrNull()?.id
    }
}