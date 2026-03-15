package com.flexynotes.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

class DriveAuthManager(private val context: Context) {

    // Configures client to request email and specific Drive AppData scope
    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

        return GoogleSignIn.getClient(context, signInOptions)
    }

    // Provides the intent required to launch the Google Sign-In UI
    fun getSignInIntent(): Intent {
        return getSignInClient().signInIntent
    }

    // Returns the currently signed-in account if available and authorized
    fun getSignedInAccount(): GoogleSignInAccount? {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null && GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_APPDATA)
            )
        ) {
            return account
        }
        return null
    }

    // Revokes access and clears the local account state
    // Revokes access and clears the local account state completely
    fun signOut(onComplete: () -> Unit) {
        val client = getSignInClient()
        // 1. Revoke the permissions from Google completely
        client.revokeAccess().addOnCompleteListener {
            // 2. Sign out locally
            client.signOut().addOnCompleteListener {
                onComplete()
            }
        }
    }
}