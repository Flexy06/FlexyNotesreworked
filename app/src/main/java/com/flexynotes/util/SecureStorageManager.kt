package com.flexynotes.util

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorageManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_sync_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Saves the password encrypted in hardware keystore
    fun saveSyncPassword(password: String) {
        sharedPreferences.edit().putString("sync_password", password).apply()
    }

    // Retrieves the decrypted password
    fun getSyncPassword(): String? {
        return sharedPreferences.getString("sync_password", null)
    }

    // Removes the password when user disables auto-sync
    fun clearSyncPassword() {
        sharedPreferences.edit().remove("sync_password").apply()
    }
}