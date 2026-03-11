package com.flexynotes.util

import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WebDavManager {

    private val client = OkHttpClient()

    // Uploads encrypted backup payload to a WebDAV server using HTTP PUT
    fun uploadBackup(
        serverUrl: String,
        username: String,
        appPassword: String,
        fileName: String,
        encryptedPayload: String
    ): Result<Unit> {
        return try {
            val credential = Credentials.basic(username, appPassword)

            // Ensure URL format is correct
            val fullUrl = if (serverUrl.endsWith("/")) "$serverUrl$fileName" else "$serverUrl/$fileName"

            // We send it as a binary stream so the server doesn't alter the text encoding
            val requestBody = encryptedPayload.toRequestBody("application/octet-stream".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", credential)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(IOException("HTTP Error: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Downloads the encrypted backup payload from a WebDAV server using HTTP GET
    fun downloadBackup(
        serverUrl: String,
        username: String,
        appPassword: String,
        fileName: String
    ): Result<String> {
        return try {
            val credential = Credentials.basic(username, appPassword)
            val fullUrl = if (serverUrl.endsWith("/")) "$serverUrl$fileName" else "$serverUrl/$fileName"

            val request = Request.Builder()
                .url(fullUrl)
                .header("Authorization", credential)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString != null) {
                        Result.success(bodyString)
                    } else {
                        Result.failure(IOException("Empty response from server"))
                    }
                } else {
                    Result.failure(IOException("HTTP Error: ${response.code} - ${response.message}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}