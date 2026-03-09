package com.flexynotes.app

import android.app.Application
import com.flexynotes.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FlexyNotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the global crash handler
        CrashReporter.init(this)
    }
}