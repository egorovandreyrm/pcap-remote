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

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.lifecycleScope
import com.google.firebase.analytics.FirebaseAnalytics
import com.vorlonsoft.android.rate.AppRate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VPN_PERMISSION_REQUEST_ACTIVITY_ID = 5
        private const val SELECT_APP_REQUEST_ACTIVITY_ID = 6
        const val INSTALL_CERTIFICATE_REQUEST_CODE = 7
        private const val SAVE_PCAP_FILE_REQUEST_ACTIVITY_ID = 8

        private const val PCAP_OK_APP_RATE_EVENT = "pcap_ok_app_rate_event"

        // in case MainActivity is fully destroyed.
        private var pcapFile: File? = null
        private var pcapPackets = 0
    }

    private var selectedApp: String? = null
    private val dashboardFragment = DashboardFragment()
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private inner class ViewPagerAdapter(manager: FragmentManager)
        : FragmentPagerAdapter(manager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        private val mFragmentList = mutableListOf<Fragment>()
        private val mFragmentTitleList = mutableListOf<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitleList[position]
        }

        internal fun addFragment(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        setSupportActionBar(toolbar)
        initAdapter()
        tlTabs.setupWithViewPager(vpTabs)

        AppRate.with(this)
                .setInstallDays(0)
                .setMinimumEventCount(PCAP_OK_APP_RATE_EVENT, 5)
                .setRemindInterval(2)
                .setRemindLaunchesNumber(1)
                .setMessage(R.string.main_activity_rate_app_dialog_message)
                //.setDebug(BuildConfig.DEBUG)
                .monitor()

        llStatus.visibility = if (SnifferVpnService.isRunning) {
            updateStatus(0, SnifferStatsEvent.SENT_TO_SSH_CLIENT_NOT_CONNECTED)
            View.VISIBLE
        } else {
            View.GONE
        }

        EventBus.getDefault().register(this)

        if (Preferences.showFirstLaunchInfo) {
            startActivity(Intent(this, InitialLaunchActivity::class.java))
            Preferences.showFirstLaunchInfo = false
        }

        MiscUtils.setDarkThemeEnabled(Preferences.darkTheme)
    }

    private fun initAdapter() {
        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFragment(dashboardFragment, getString(R.string.main_activity_dashboard_tab_title))
        adapter.addFragment(SslErrorsFragment(), getString(R.string.main_activity_ssl_errors_tab_title))
        vpTabs.adapter = adapter
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        menu.findItem(R.id.action_start_capturing)?.isVisible = !SnifferVpnService.isRunning
        menu.findItem(R.id.action_start_capturing_single_app)?.isVisible = !SnifferVpnService.isRunning
        menu.findItem(R.id.action_stop_capturing)?.isVisible = SnifferVpnService.isRunning
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_start_capturing -> {
                selectedApp = null
                startCapturing()
            }
            R.id.action_start_capturing_single_app -> {
                showSelectSingleAppFragment()
            }
            R.id.action_stop_capturing -> {
                stopCapturing()
            }
            R.id.action_info -> {
                MiscUtils.startHelpActivity(this)
            }
            R.id.action_settings -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.action_display_network_interfaces -> {
                showNetworkInterfacesDialog()
            }
            R.id.action_feedback -> sendEmail()
            R.id.action_rate_app -> rateApp()
//            R.id.action_support_app -> {
//                AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogTheme)
//                        .setTitle(R.string.main_activity_support_the_project_dialog_title)
//                        .setMessage(R.string.initial_launch_activity_support_the_project)
//                        .setPositiveButton(R.string.main_activity_support_the_project_dialog_purchase) { _, _ ->
//
//                        }
//                        .setNegativeButton(R.string.main_activity_support_the_project_dialog_maybe_later, null)
//                        .show()
//            }

            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun showNetworkInterfacesDialog() {
        lifecycleScope.launch {
            var networkInterfaces = withContext(Dispatchers.IO) {
                IpUtils.getNetworkInterfacesString()
            }

            if (networkInterfaces.isEmpty()) {
                networkInterfaces = getString(R.string.dashboard_failed_to_read_network_interfaces)
            }

            AlertDialog.Builder(this@MainActivity, R.style.MyAlertDialogTheme)
                    .setTitle(R.string.main_activity_network_interfaces_dialog_title)
                    .setMessage(networkInterfaces)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(R.string.main_activity_network_interfaces_dialog_copy) { _, _ ->
                        val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(networkInterfaces, networkInterfaces))
                        Toast.makeText(
                                this@MainActivity,
                                R.string.main_activity_copied_to_clipboard,
                                Toast.LENGTH_SHORT).show()

                    }
                    .show()
        }
    }

    private fun showSelectSingleAppFragment() {
        startActivityForResult(
                Intent(this, SelectAppActivity::class.java),
                SELECT_APP_REQUEST_ACTIVITY_ID)
    }

    private fun startCapturing() {
        val intent = VpnService.prepare(this)
        if (null == intent) {
            SslErrorsFragment.clearLog()

            pcapFile = if (Preferences.remoteMode) {
                null
            } else {
                File(cacheDir, generatePcapFileName())
            }

            pcapFile?.createNewFile()

            if (Preferences.remoteMode) {
                if (Preferences.showFirstRemoteCapturingInfo) {

                    AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                            .setTitle(R.string.info_dialog_title)
                            .setMessage(R.string.main_activity_first_remote_capturing_info)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(R.string.tutorial_dialog_open_help_howto) { _, _ ->
                                MiscUtils.startHelpActivity(this)
                            }
                            .show()

                    Preferences.showFirstRemoteCapturingInfo = false
                } else {
                    Toast.makeText(
                            this,
                            getString(R.string.main_activity_remote_capturing_started_toast),
                            Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.main_activity_capturing_started_toast),
                        Toast.LENGTH_LONG).show()
            }

            SnifferVpnService.startCapturing(this, pcapFile?.absolutePath, selectedApp)
        } else {
            startActivityForResult(intent, VPN_PERMISSION_REQUEST_ACTIVITY_ID)
        }
    }

    private fun generatePcapFileName(): String {
        val date = Date(System.currentTimeMillis())
        val simpleDateFormat = SimpleDateFormat("yyyyMMddhhmmss", Locale.ENGLISH)

        var fileName = "${simpleDateFormat.format(date)}.pcap"

        selectedApp?.let {
            val formattedAppName = it.replace(".", "_")
            fileName = formattedAppName + "_" + fileName
        }

        return fileName
    }

    private fun stopCapturing() {
        SnifferVpnService.stopCapturing(this)

        val dataCaptured = (null != selectedApp && 30 < pcapPackets) || 100 < pcapPackets
        if (dataCaptured) {
            AppRate.with(this).incrementEventCount(PCAP_OK_APP_RATE_EVENT)
        }

        if (dataCaptured) {
            val bundle = Bundle()
            bundle.putBoolean("ssl_mitm", Preferences.sslMitm)

            if (Preferences.remoteMode) {
                bundle.putBoolean("drop_connections_on_ssh_client", Preferences.dropConnectionsOnSshClient)
                firebaseAnalytics.logEvent("ssh_server_pcap_captured_data", bundle)
            } else {
                firebaseAnalytics.logEvent("file_pcap_captured_data", bundle)
            }

        }

        if (Preferences.remoteMode) {
            showRateAppDialog()
        } else {
            pcapFile?.let { file ->
                if (file.exists()) {
                    AlertDialog.Builder(this)
                            .setCancelable(true)
                            .setTitle(R.string.main_activity_save_pcap_file_dialog_title)
                            .setMessage(R.string.main_activity_save_pcap_file_dialog_message)
                            .setPositiveButton(R.string.main_activity_save_pcap_file_dialog_save_button) { _, _ ->
                                startSavePcapActivity(file.name)
                            }
                            .setNegativeButton(R.string.main_activity_save_pcap_file_dialog_discard_button) { _, _ ->
                                file.delete()
                            }
                            .setOnDismissListener {
                                showRateAppDialog()
                            }
                            .create().show()
                }
            }
        }
    }

    private fun showRateAppDialog() {
        if (AppRate.showRateDialogIfMeetsConditions(this)) {
            MiscUtils.analyticsLogSelectContent(firebaseAnalytics, "rate_app_dialog_displayed", null)
        }
    }

    private fun startSavePcapActivity(fileName: String) {
        try {
            val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
            exportIntent.type = "application/octet-stream"
            exportIntent.putExtra(Intent.EXTRA_TITLE, fileName)
            startActivityForResult(exportIntent, SAVE_PCAP_FILE_REQUEST_ACTIVITY_ID)
        } catch (ex: Exception) {
            Timber.e(ex)
            ex.message?.let { Toast.makeText(this, it, Toast.LENGTH_LONG).show() }
        }
    }

    private fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.type = "text/plain"
        intent.data = Uri.parse("mailto:egorovandreyrm@gmail.com")

        intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Feedback: PCAP Remote: ${MiscUtils.appVersion(this)}")

        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    private fun rateApp() {
        MiscUtils.openPlayStore(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Timber.i("onActivityResult: %d", requestCode)

        if (Activity.RESULT_OK != resultCode) {
            return
        }

        when (requestCode) {
            VPN_PERMISSION_REQUEST_ACTIVITY_ID -> {
                startCapturing()
            }

            SELECT_APP_REQUEST_ACTIVITY_ID -> {
                data?.getStringExtra(SelectAppActivity.SELECTED_APP_EXTRA)?.let {
                    selectedApp = it
                    startCapturing()
                }
            }

            INSTALL_CERTIFICATE_REQUEST_CODE -> {
                Preferences.mitmCertInstalled = true
            }

            SAVE_PCAP_FILE_REQUEST_ACTIVITY_ID -> {
                data?.data?.let { uri ->
                    pcapFile?.let { file ->
                        try {
                            FileInputStream(file).use { inputStream ->
                                contentResolver.openOutputStream(uri)?.use { outputStream ->
                                    MiscUtils.copyStream(inputStream, outputStream)
                                }
                            }

                            file.delete()

                            Toast.makeText(
                                    this,
                                    getString(R.string.main_activity_pcap_file_saved_toast),
                                    Toast.LENGTH_LONG).show()
                        } catch (ex: Exception) {
                            val error = String.format(getString(R.string.main_activity_save_pcap_file_error), ex.message)
                            Timber.e(error)
                            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    fun onSnifferRunningStateChanged(event: SnifferRunningStateChanged) {
        invalidateOptionsMenu()

        llStatus.visibility = if (SnifferVpnService.isRunning) {
            updateStatus(0, SnifferStatsEvent.SENT_TO_SSH_CLIENT_NOT_CONNECTED)
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSnifferStatsEvent(event: SnifferStatsEvent) {
        if (!SnifferVpnService.isRunning) {
            Timber.e("sniffer vpn service is not running")
            return
        }

        updateStatus(event.capturedPackets, event.sentToSsh)
    }

    private fun updateStatus(capturedPackets: Int, sentToSsh: Int) {
        tvStatus.text = MiscUtils.getStatusString(this, capturedPackets, sentToSsh)

        if (Preferences.remoteMode) {
            if (capturedPackets < sentToSsh) {
                pcapPackets = sentToSsh
            }
        } else {
            pcapPackets = capturedPackets
        }
    }
}
