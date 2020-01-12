package com.pcapremote

import android.content.Context
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.*

object Logcat {
    var isRunning: Boolean = false
    private var logcatProcess: Process? = null
    private var killLogcatProcess: Process? = null

    fun getFile(context: Context): File {
        return File(context.cacheDir, "logcat.txt")
    }

    fun startCapturing(context: Context): Boolean {
        if (isRunning) {
            Timber.w("startCapturing: capturing is already running")
            return true
        }

        val file = getFile(context)
        if (file.exists()) {
            file.delete()
        }

        try {
            val cmd = String.format(Locale.ENGLISH, "logcat -f %s", file.absolutePath)
            logcatProcess = Runtime.getRuntime().exec(cmd)
        } catch (e: IOException) {
            Timber.e(e)
            return false
        }

        try {
            val pid = android.os.Process.myPid()
            val script = "/system/bin/sh - c while [ -d /proc/$pid ];do sleep 1;done; killall logcat"
            killLogcatProcess = Runtime.getRuntime().exec(script)
        } catch (e: IOException) {
            Timber.e(e)
        }

        Timber.d("startCapturing: capturing has been started")

        isRunning = true
        return isRunning
    }

    fun stopCapturing() {
        isRunning = false

        logcatProcess?.destroy()

        try {
            Runtime.getRuntime().exec("killall logcat")
        } catch (ex: IOException) {
            Timber.e(ex)
        }

        killLogcatProcess?.destroy()

        Timber.d("logcat capturing has been stopped")
    }
}