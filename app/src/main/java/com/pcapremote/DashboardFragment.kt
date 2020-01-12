package com.pcapremote

import android.app.AlertDialog
import android.content.*
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.capturing_mode_spinner_item.view.*
import kotlinx.android.synthetic.main.fragment_dashboard.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class DashboardFragment : Fragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private companion object {
        const val SSH_SERVER_CAPTURING_MODE_INDEX = 0
        const val PCAP_FILE_CAPTURING_MODE_INDEX = 1
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateSshServerIpView()
        }

        override fun onLost(network: Network?) {
            updateSshServerIpView()
        }
    }

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            updateSshServerIpView()
        }
    }

    private data class CapturingMode(val name: String, val details: String)

    private class CapturingModesAdapter(context: Context, modes: List<CapturingMode>)
        : ArrayAdapter<CapturingMode>(context, 0, modes) {

        override fun getView(position: Int, recycledView: View?, parent: ViewGroup): View {
            return this.createView(position, recycledView, parent)
        }

        override fun getDropDownView(position: Int, recycledView: View?, parent: ViewGroup): View {
            return this.createView(position, recycledView, parent)
        }

        private fun createView(position: Int, recycledView: View?, parent: ViewGroup): View {
            val view = recycledView ?: LayoutInflater.from(context).inflate(
                    R.layout.capturing_mode_spinner_item,
                    parent,
                    false
            )

            getItem(position)?.let {
                view.tvCapturingModeName.text = it.name
                view.tvCapturingModeDetails.text = it.details
            }

            return view
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val capturingModes = mutableListOf<CapturingMode>()
        capturingModes.add(CapturingMode(
                getString(R.string.dashboard_capturing_mode_ssh_server_title),
                getString(R.string.dashboard_capturing_mode_ssh_server_description)))

        capturingModes.add(CapturingMode(
                getString(R.string.dashboard_capturing_mode_pcap_file_title),
                getString(R.string.dashboard_capturing_mode_pcap_file_description)))

        spnCapturingModes.adapter = CapturingModesAdapter(requireContext(), capturingModes)

        if (Preferences.remoteMode) {
            spnCapturingModes.setSelection(SSH_SERVER_CAPTURING_MODE_INDEX)
        } else {
            spnCapturingModes.setSelection(PCAP_FILE_CAPTURING_MODE_INDEX)
        }

        switchSslMitm.isChecked = Preferences.sslMitm
        switchDropConnectionsOnSshClient.isChecked = Preferences.dropConnectionsOnSshClient

        spnCapturingModes.isEnabled = !SnifferVpnService.isRunning
        switchSslMitm.isEnabled = !SnifferVpnService.isRunning
        switchDropConnectionsOnSshClient.isEnabled = !SnifferVpnService.isRunning && Preferences.remoteMode

        updateSshServerIpView()
        initSshServerPortView()
        setSshServerViewsVisibility(spnCapturingModes.selectedItemPosition == SSH_SERVER_CAPTURING_MODE_INDEX)

        spnCapturingModes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isRemoteMode = (position == SSH_SERVER_CAPTURING_MODE_INDEX)
                Preferences.remoteMode = isRemoteMode
                setSshServerViewsVisibility(isRemoteMode)
            }
        }

        switchSslMitm.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
            Preferences.sslMitm = b

            val installCertificateLambda = {
                if (b && !Preferences.mitmCertInstalled) {
                    renderInstallMitmCertDialog()
                }
            }

            if (b && Preferences.showSslMitmWarning) {
                AlertDialog.Builder(requireActivity(), R.style.MyAlertDialogTheme)
                        .setTitle(R.string.warn_dialog_title)
                        .setMessage(R.string.dashboard_ssl_mitm_warning)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener {
                            renderInstallMitmCertDialog()
                        }
                        .show()

                Preferences.showSslMitmWarning = false
            } else {
                installCertificateLambda()
            }
        }

        switchDropConnectionsOnSshClient.setOnCheckedChangeListener { _: CompoundButton, b: Boolean ->
            Preferences.dropConnectionsOnSshClient = b
        }

        registerConnectionChangedListener()
        Preferences.preferences.registerOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().register(this)
    }

    override fun onDestroyView() {
        unregisterConnectionChangedListener()
        Preferences.preferences.unregisterOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().unregister(this)

        super.onDestroyView()
    }

    private fun renderInstallMitmCertDialog() {
        val context = activity ?: return

        AlertDialog.Builder(context, R.style.MyAlertDialogTheme)
                .setTitle(R.string.main_activity_mitm_certificate_warning_title)
                .setMessage(R.string.main_activity_mitm_certificate_warning)
                .setPositiveButton(R.string.main_activity_mitm_certificate_warning_install_action) { _, _ ->
                    context.startActivityForResult(
                            SslCerts.createInstallMitmCertIntent(context),
                            MainActivity.INSTALL_CERTIFICATE_REQUEST_CODE)
                }.setNegativeButton(R.string.main_activity_mitm_certificate_warning_ignore_action, null).show()
    }

    private fun registerConnectionChangedListener() {
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cm = it.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                cm?.registerDefaultNetworkCallback(networkCallback)
            } else {
                it.registerReceiver(networkReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
            }
        }
    }

    private fun unregisterConnectionChangedListener() {
        context?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
                cm?.unregisterNetworkCallback(networkCallback)
            } else {
                it.unregisterReceiver(networkReceiver)
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.preferences_ssh_server_port)) {
            initSshServerPortView()
        }
    }

    private fun initSshServerPortView() {
        tvSshServerPort.text = String.format(
                getString(R.string.dashboard_ssh_server_port),
                Preferences.sshPort)
    }

    private fun setSshServerViewsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        switchDropConnectionsOnSshClient.visibility = visibility
        tvSshServer.visibility = visibility
        vSshServerDelimiter.visibility = visibility
        llSshServerInfo.visibility = visibility
    }

    private fun updateSshServerIpView() {
        viewLifecycleOwner.lifecycleScope.launch {
            var wifiIpV4 = withContext(Dispatchers.IO) {
                IpUtils.wifiIpV4Addr
            }

            if (wifiIpV4.isNullOrEmpty()) {
                wifiIpV4 = getString(R.string.dashboard_ssh_server_ip_none)
            }

            tvSshServerIpv4.text = String.format(getString(R.string.dashboard_ssh_server_ip), wifiIpV4)
        }
    }

    @Subscribe
    fun onSnifferRunningStateChanged(event: SnifferRunningStateChanged) {
        spnCapturingModes.isEnabled = !SnifferVpnService.isRunning
        switchSslMitm.isEnabled = !SnifferVpnService.isRunning
        switchDropConnectionsOnSshClient.isEnabled = !SnifferVpnService.isRunning
    }
}
