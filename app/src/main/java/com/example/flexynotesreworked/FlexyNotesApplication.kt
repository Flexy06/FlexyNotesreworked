package com.example.flexynotesreworked

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Triggers Hilt's code generation, including a base class for your application
@HiltAndroidApp
class FlexyNotesApplication : Application()