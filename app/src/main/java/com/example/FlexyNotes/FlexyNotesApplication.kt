package com.example.FlexyNotes

import android.app.Application
import com.example.FlexyNotes.util.CrashReporter
import dagger.hilt.android.HiltAndroidApp

// Triggers Hilt's code generation, including a base class for your application
@HiltAndroidApp
class FlexyNotesApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the global crash handler
        CrashReporter.init(this)
    }
}