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

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.widget.Toast
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import timber.log.Timber
import java.io.IOException
import java.net.InetSocketAddress
import java.util.*

class SnifferVpnService : VpnService() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var vpnPfd: ParcelFileDescriptor? = null
    private lateinit var connectivityManager: ConnectivityManager
    private var singleApp: String? = null


    @Suppress("unused")
    private fun jniOnStatsUpdated(capturedPackets: Int, sentToSsh: Int) {
        EventBus.getDefault().post(SnifferStatsEvent(capturedPackets, sentToSsh))

        serviceScope.launch {
            updateNotification(capturedPackets, sentToSsh)
        }
    }

    @Suppress("unused")
    private fun jniOnSllError(
            srcPort: Int,
            ipAddr: String,
            hostname: String?,
            port: Int,
            appId: Int,
            reason: String) {

        val error = String.format(
                Locale.ENGLISH,
                getString(R.string.pcap_vpn_service_ssl_error),
                getAppName(srcPort, ipAddr, port, appId),
                hostname,
                reason)

        SslErrorsFragment.addEntry(error)
    }

    private fun getAppName(srcPort: Int, ipAddr: String, port: Int, appId: Int): String {
        singleApp?.let {
            return it
        }

        var updatedAppId = appId
        if (-1 == updatedAppId && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val remoteAddr = InetSocketAddress(ipAddr, port)
            val ipv6 = (-1 != remoteAddr.address.hostAddress.indexOf(':'))
            updatedAppId = connectivityManager.getConnectionOwnerUid(
                    OsConstants.IPPROTO_TCP,
                    InetSocketAddress(if (ipv6) LOCAL_ADDR_IP6 else LOCAL_ADDR_IP4, srcPort),
                    remoteAddr)
        }

        if (-1 != updatedAppId) {
            getPackageNameForUid(updatedAppId)?.let { return it }
        }

        return "unknown"
    }

    @Suppress("unused")
    private fun jniOnStopped(reason: String) {
        stopService()
        logUnexpectedlyStoppedError(reason)
    }

    @Suppress("unused")
    private fun jniOnSocketCreated(socket: Int): Boolean {
        return null != vpnPfd && protect(socket)
    }

    private external fun jniSetCerts(mitmCert: String, mitmKey: String, sshKeyFilePath: String)

    private external fun jniStartFilePcap(tun: Int, mtu: Int, sslMitm: Boolean, pcapFilePath: String): Boolean

    private external fun jniStartSshServerPcap(
            tun: Int, mtu: Int, sslMitm: Boolean, dropConnectionOnSshClient: Boolean, port: Int): Boolean

    private external fun jniDispose()

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")
        isRunning = true
        EventBus.getDefault().post(SnifferRunningStateChanged())

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        try {
            startForeground()
        } catch (ex: Exception) {
            Timber.e("startForeground: ${ex.message}")
        }

        if (!jniCertsInitialized) {
            jniSetCerts(
                    SslCerts.getMitmCert(this),
                    SslCerts.getMitmKey(this),
                    SslCerts.getSshKeyFile(this).absolutePath)

            jniCertsInitialized = true
        }
    }

    private fun startForeground() {
        val status = MiscUtils.getStatusString(
                this,
                0,
                SnifferStatsEvent.SENT_TO_SSH_CLIENT_NOT_CONNECTED)

        startForeground(FOREGROUND_ID_, getNotification(status))
    }

    private fun updateNotification(capturedPackets: Int, sentToSsh: Int) {
        val status = MiscUtils.getStatusString(this, capturedPackets, sentToSsh)

        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)?.
                notify(FOREGROUND_ID_, getNotification(status))
    }

    private fun getNotification(contentText: String): Notification {
        val builder = if (MiscUtils.isAtLeastO()) {
            Notification.Builder(this, createNotificationChannel(this))
        } else {
            Notification.Builder(this)
        }

        builder.setSmallIcon(R.mipmap.pcapremote_notification)

        builder.setContentTitle(getString(R.string.pcap_vpn_service_foreground_notification_title))
        builder.setContentText(contentText)


        val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setContentIntent(pendingIntent)

        return builder.build()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val startService = intent.getBooleanExtra(START_SERVICE_EXTRA, true)

        serviceScope.launch(Dispatchers.IO) {
            if (startService) {
                if (!startSniffer(intent)) {
                    Timber.e("failed to start sniffer")
                    stopService()
                }
            } else {
                stopService()
            }
        }

        return if (startService) Service.START_REDELIVER_INTENT else Service.START_NOT_STICKY
    }

    private fun startSniffer(intent: Intent): Boolean {
        try {
            val app = intent.getStringExtra(APP_EXTRA)
            val tun = openTun(app)
            if (null != tun) {
                if (Preferences.remoteMode) {
                    jniStartSshServerPcap(
                            tun.fd,
                            MTU,
                            Preferences.sslMitm,
                            Preferences.dropConnectionsOnSshClient,
                            Preferences.sshPort)
                } else {
                    val pcapFilePath = intent.getStringExtra(PCAP_FILE_PATH_EXTRA)
                    if (pcapFilePath.isNullOrEmpty()) {
                        return false
                    }

                    jniStartFilePcap(tun.fd, MTU, Preferences.sslMitm, pcapFilePath)
                }

                return true
            } else {
                logUnexpectedlyStoppedError("failed to open tun ifr")
            }
        } catch (ex: Exception) {
            logUnexpectedlyStoppedError(ex.message
                    ?: "unexpected exception while initializing sniffer")
        }

        return false
    }

    override fun onRevoke() {
        Timber.d("onRevoke")
        super.onRevoke()
    }

    override fun onDestroy() {
        Timber.i("onDestroy")
        serviceJob.cancel()
        jniDispose()
        closeTun()

        isRunning = false
        EventBus.getDefault().post(SnifferRunningStateChanged())

        super.onDestroy()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createNotificationChannel(context: Context): String {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = context.getString(R.string.app_channel)
        if (notificationManager.getNotificationChannel(channelId) == null) {
            val notificationChannel = NotificationChannel(
                    channelId,
                    context.getString(R.string.pcap_vpn_service_foreground_notification_title),
                    NotificationManager.IMPORTANCE_DEFAULT)

            notificationChannel.setSound(null,null)
            notificationChannel.enableLights(false)
            notificationChannel.lightColor = Color.BLUE
            notificationChannel.enableVibration(false)

            notificationManager.createNotificationChannel(notificationChannel)
        }
        return channelId
    }

    private fun stopService() {
        jniDispose()
        closeTun()
        stopSelf()
    }

    private fun openTun(app: String?): ParcelFileDescriptor? {
        val builder = Builder()
        builder.setSession("pcapremote")

        builder.addAddress(LOCAL_ADDR_IP4, 32)
        builder.addAddress(LOCAL_ADDR_IP6, 128) // ipv6

        builder.addRoute("0.0.0.0", 0)
        builder.addRoute("2000::", 3) // unicast

        builder.setMtu(MTU)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (dnsAddr in getAndroid10DnsList()) {
                builder.addDnsServer(dnsAddr)
            }
        }

        singleApp = app

        if (null == app) {
            try {
                builder.addDisallowedApplication(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.w(e)
            }
        } else {
            Timber.d("capturing traffic of a single app: $app")
            try {
                builder.addAllowedApplication(app)
            } catch (e: PackageManager.NameNotFoundException) {
                Timber.w(e)
            }
        }

        vpnPfd = builder.establish()

        if (null == vpnPfd) {
            Toast.makeText(
                    this,
                    R.string.pcap_vpn_service_establish_failed,
                    Toast.LENGTH_LONG).show()
        }

        return vpnPfd
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun getAndroid10DnsList(): List<String> {
        val dnsList = mutableListOf<String>()

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.activeNetwork?.let { activeNetwork ->
            connectivityManager.getLinkProperties(activeNetwork)?.let {
                for (d in it.dnsServers) {
                    Timber.i("DNS from LP: ${d.hostAddress}")
                    dnsList.add(d.hostAddress.split("%").toTypedArray()[0])
                }
            }
        }

        if (dnsList.isEmpty()) {
            dnsList.add("8.8.8.8")
            dnsList.add("2001:4860:4860::8888")
        }

        return dnsList
    }

    private fun closeTun() {
        vpnPfd?.let {
            try {
                it.close()
            } catch (e: IOException) {
                Timber.w(e)
            }

            vpnPfd = null
        }
    }

    private fun getPackageNameForUid(uid: Int): String? {
        if (-1 != uid) {
            val packageNames = packageManager.getPackagesForUid(uid)
            if (null != packageNames && packageNames.isNotEmpty()) {
                return packageNames[0]
            }
        }
        return null
    }

    private fun logUnexpectedlyStoppedError(reason: String) {
        serviceScope.launch {
            val error = String.format(
                    Locale.ENGLISH,
                    getString(R.string.pcap_vpn_service_jni_unexpectedly_stopped),
                    reason)

            Timber.e(error)
            Toast.makeText(this@SnifferVpnService, error, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        init {
            System.loadLibrary("pcapremote")
        }

        var isRunning: Boolean = false
        private var jniCertsInitialized: Boolean = false

        private const val FOREGROUND_ID_ = 557
        private const val MTU = 10000
        private const val LOCAL_ADDR_IP4 = "10.1.10.1"
        private const val LOCAL_ADDR_IP6 = "fd00:1:fd00:1:fd00:1:fd00:1"

        private const val START_SERVICE_EXTRA = "startService"
        private const val PCAP_FILE_PATH_EXTRA = "pcapFileName"
        private const val APP_EXTRA = "app"

        fun startCapturing(context: Context, pcapFilePath: String?, app: String?) {
            val intent = Intent(context, SnifferVpnService::class.java)

            check(Preferences.remoteMode || !pcapFilePath.isNullOrEmpty()) {
                "pcap file name is not provided"
            }

            pcapFilePath?.let { intent.putExtra(PCAP_FILE_PATH_EXTRA, pcapFilePath) }
            app?.let { intent.putExtra(APP_EXTRA, app) }

            if (MiscUtils.isAtLeastO()) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopCapturing(context: Context) {
            Timber.d("stopping capturing...")

            val intent = Intent(context, SnifferVpnService::class.java)
            intent.putExtra(START_SERVICE_EXTRA, false)
            context.startService(intent)
        }

        external fun setInfoLogEnabled(value: Boolean)
        external fun setWarnLogEnabled(value: Boolean)
        external fun setErrorLogEnabled(value: Boolean)
        external fun setDebugLogEnabled(value: Boolean)
    }
}
