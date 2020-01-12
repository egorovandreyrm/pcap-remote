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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.*

object MiscUtils {

    fun setDarkThemeEnabled(isEnabled: Boolean) {
        val nightMode = if (isEnabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun isSamsung(): Boolean {
        return Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("samsung")
    }

    fun getStatusString(context: Context, capturedPackets: Int, sentToSsh: Int): String {
        return if (Preferences.remoteMode) {
            if (SnifferStatsEvent.SENT_TO_SSH_CLIENT_NOT_CONNECTED == sentToSsh) {
                context.getString(R.string.capturing_status_awaiting_for_ssh_client)
            } else {
                String.format(
                        context.getString(R.string.capturing_status_remote_capturing),
                        capturedPackets,
                        sentToSsh)
            }
        } else {
            String.format(
                    context.getString(R.string.capturing_status_capturing),
                    capturedPackets)
        }
    }

    fun analyticsLogSelectContent(firebaseAnalytics: FirebaseAnalytics, itemId: String, contentType: String?) {
        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, itemId)
        contentType?.let { bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, it) }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
    }

    fun renderErrorDialog(context: Context, error: String) {
        Timber.e(error)
        AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                .setTitle(R.string.error_dialog_title)
                .setMessage(error)
                .setPositiveButton(android.R.string.ok, null).show()
    }

    fun startHelpActivity(context: Context) {
        val url = "https://egorovandreyrm.com/pcap-remote-tutorial/"

        val intent = Intent(context, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.TITLE_EXTRA, context.getString(R.string.webview_activity_info_title))
        intent.putExtra(WebViewActivity.URL_EXTRA, url)

        intent.putExtra(
                WebViewActivity.SHARED_INFO_TEMPLATE_EXTRA,
                context.getString(R.string.webview_activity_info_shared_info_template))

        context.startActivity(intent)
    }

    fun startPrivacyPolicyActivity(context: Context) {
        val url = "https://egorovandreyrm.com/pcap-remote-privacy-policy/"

        val intent = Intent(context, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.TITLE_EXTRA, context.getString(R.string.webview_activity_privacy_policy_title))
        intent.putExtra(WebViewActivity.URL_EXTRA, url)
        context.startActivity(intent)
    }

    fun openPlayStore(context: Context) {
        val appPackageName = context.packageName
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
        } catch (ex: android.content.ActivityNotFoundException) {
            Timber.e(ex)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
        }
    }

    fun appVersion(context: Context): String {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (isAtLeastP()) pInfo.longVersionCode else pInfo.versionCode.toLong()

            return String.format(
                    context.getString(R.string.preferences_about_version_template),
                    pInfo.versionName, versionCode.toString())

        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e(e)
        } catch (e: Throwable) {
            Timber.e(e)
        }

        return "unknown"
    }

    fun isAtLeastO(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    }

    fun isAtLeastP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    fun readFileFromAsset(context: Context, filePath: String): String {
        InputStreamReader(context.assets.open(filePath)).use {
            return it.readText()
        }
    }

    fun copyStream(inputStream: InputStream, outputStream: OutputStream) {
        val data = ByteArray(4096)
        while (true) {
            val count = inputStream.read(data)
            if (-1 == count) {
                break
            }

            outputStream.write(data, 0, count)
        }
    }
}