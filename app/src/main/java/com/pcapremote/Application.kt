package com.pcapremote

import android.util.Log
import androidx.room.Room
import timber.log.Timber


/**
 * Created by andrey on 17.09.17.
 */

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
