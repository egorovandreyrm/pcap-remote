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

import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Preferences {

    private var preferencesInstance: SharedPreferences? = null
    val preferences: SharedPreferences
        get() {
            preferencesInstance?.let { return it }

            PreferenceManager.setDefaultValues(Application.instance, R.xml.settings, false)
            val instance = PreferenceManager.getDefaultSharedPreferences(Application.instance)
            preferencesInstance = instance
            return instance
        }

    interface Properties {
        companion object {
            const val SHOW_FIRST_LAUNCH_INFO = "show_first_launch_info"
            const val SHOW_SSL_MITM_WARNING = "show_ssl_mitm_warning"
            const val SHOW_FIRST_REMOTE_CAPTURING_INFO = "show_first_remote_capturing_info"
            const val REMOTE_MODE = "remote_mode"
            const val SSL_MITM = "ssl_mitm"
            const val DROP_CONNECTIONS_ON_SSH_CLIENT = "drop_connections_on_ssh_client"
            const val MITM_CERT_INSTALLED = "mitm_cert_installed"
            const val LAST_SELECTED_SINGLE_APP_TO_CAPTURE = "last_selected_single_app_to_capture"
            const val DEBUG_MODE = "debug_mode"
            const val DARK_THEME = "dark_theme"
        }
    }

    var showSslMitmWarning: Boolean
        get() = preferences.getBoolean(Properties.SHOW_SSL_MITM_WARNING, true)
        set(value) {
            preferences.edit().putBoolean(Properties.SHOW_SSL_MITM_WARNING, value).apply()
        }

    var showFirstLaunchInfo: Boolean
        get() = preferences.getBoolean(Properties.SHOW_FIRST_LAUNCH_INFO, true)
        set(value) {
            preferences.edit().putBoolean(Properties.SHOW_FIRST_LAUNCH_INFO, value).apply()
        }

    var showFirstRemoteCapturingInfo: Boolean
        get() = preferences.getBoolean(Properties.SHOW_FIRST_REMOTE_CAPTURING_INFO, true)
        set(value) {
            preferences.edit().putBoolean(Properties.SHOW_FIRST_REMOTE_CAPTURING_INFO, value).apply()
        }

    var remoteMode: Boolean
        get() = preferences.getBoolean(Properties.REMOTE_MODE, true)
        set(value) {
            preferences.edit().putBoolean(Properties.REMOTE_MODE, value).apply()
        }

    var sslMitm: Boolean
        get() = preferences.getBoolean(Properties.SSL_MITM, false)
        set(value) {
            preferences.edit().putBoolean(Properties.SSL_MITM, value).apply()
        }

    var dropConnectionsOnSshClient: Boolean
        get() = preferences.getBoolean(Properties.DROP_CONNECTIONS_ON_SSH_CLIENT, true)
        set(value) {
            preferences.edit().putBoolean(Properties.DROP_CONNECTIONS_ON_SSH_CLIENT, value).apply()
        }

    var mitmCertInstalled: Boolean
        get() = preferences.getBoolean(Properties.MITM_CERT_INSTALLED, false)
        set(value) = preferences.edit().putBoolean(Properties.MITM_CERT_INSTALLED, value).apply()

    var sshPort: Int
        get() {
            val defaultValue = Application.instance.getString(R.string.preferences_default_ssh_server_port)
            return Integer.parseInt(preferences.getString(
                    Application.instance.getString(R.string.preferences_keys_ssh_server_port),
                    defaultValue)!!)
        }
        set(value) {
            preferences.edit().putInt(
                    Application.instance.getString(R.string.preferences_keys_ssh_server_port), value).apply()
        }

    // the latest one is at the end of the list.
    var lastSelectedSingleAppsToCapture: List<String>
        get() {
            val value = preferences.getString(Properties.LAST_SELECTED_SINGLE_APP_TO_CAPTURE, null)
            return value?.split(",")?.map { it.trim() } ?: return listOf()
        }
        set(value) {
            preferences.edit().putString(
                    Properties.LAST_SELECTED_SINGLE_APP_TO_CAPTURE, value.joinToString()).apply()
        }

    var debugMode: Boolean
        get() = preferences.getBoolean(Properties.DEBUG_MODE, false)
        set(value) {
            preferences.edit().putBoolean(Properties.DEBUG_MODE, value).apply()
        }

    var darkTheme: Boolean
        get() = preferences.getBoolean(Properties.DARK_THEME, false)
        set(value) {
            preferences.edit().putBoolean(Properties.DARK_THEME, value).apply()
        }
}
