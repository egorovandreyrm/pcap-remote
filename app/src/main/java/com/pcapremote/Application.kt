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

import android.util.Log
import androidx.room.Room
import timber.log.Timber

class Application : android.app.Application() {

    private class ErrorsTimberTree : Timber.DebugTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (Log.ERROR != priority) {
                return
            }
            super.log(priority, tag, message, t)
        }
    }

    lateinit var sslErrorsDao: SslErrorsDao

    override fun onCreate() {
        super.onCreate()
        instance = this

        Preferences.debugMode = false

        sslErrorsDao = Room.databaseBuilder(
                applicationContext,
                SslErrorsDatabase::class.java, "ssl_errors.db"
        ).build().sslErrorDao()

        initializeLog()
    }

    private fun initializeLog() {
        Timber.uprootAll()

        Timber.plant(if (BuildConfig.DEBUG || Preferences.debugMode)
            Timber.DebugTree() else ErrorsTimberTree())

        SnifferVpnService.setInfoLogEnabled(BuildConfig.DEBUG || Preferences.debugMode)
        SnifferVpnService.setWarnLogEnabled(BuildConfig.DEBUG || Preferences.debugMode)
        SnifferVpnService.setErrorLogEnabled(true)
        SnifferVpnService.setDebugLogEnabled(BuildConfig.DEBUG || Preferences.debugMode)
    }

    fun enableDebugMode() {
        if (Preferences.debugMode) {
            Timber.d("debug mode is already enabled")
            return
        }

        Preferences.debugMode = true
        initializeLog()
    }

    companion object {
        lateinit var instance: Application
    }
}
