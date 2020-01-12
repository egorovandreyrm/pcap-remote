/*
    This file is part of PCAP Remote.

    PCAP Remote is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    PCAP Remote is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with PCAP Remote. If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019 by Andrey Egorov
*/

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