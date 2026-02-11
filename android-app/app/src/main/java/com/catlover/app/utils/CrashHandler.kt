package com.catlover.app.utils

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            saveCrashReport(thread, throwable)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error saving crash report", e)
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashReport(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val fileName = "crash_" + timestamp + ".txt"
        val crashFile = File(context.filesDir, fileName)

        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTrace = sw.toString()

            val writer = FileWriter(crashFile)
            writer.append("Timestamp: " + timestamp + "\n")
            writer.append("Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n")
            writer.append("Android Version: " + Build.VERSION.RELEASE + "\n")
            writer.append("Thread: " + thread.name + "\n")
            writer.append("--- Stack Trace ---\n")
            writer.append(stackTrace)
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
