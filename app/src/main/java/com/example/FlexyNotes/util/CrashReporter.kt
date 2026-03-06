package com.flexynotes.util

import android.content.Context
import java.io.File
import kotlin.system.exitProcess

class CrashReporter(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // Extract the full stack trace
            val stackTrace = throwable.stackTraceToString()

            // Save it to a local file
            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(stackTrace)
        } catch (e: Exception) {
            // Ignore errors while trying to write the crash log
        } finally {
            // Let the default Android crash handler take over and close the app
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
    }

    companion object {
        private const val CRASH_FILE_NAME = "last_crash_log.txt"

        fun init(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashReporter(context.applicationContext))
        }

        fun getCrashLog(context: Context): String? {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            return if (file.exists()) file.readText() else null
        }

        fun clearCrashLog(context: Context) {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        }
    }
}