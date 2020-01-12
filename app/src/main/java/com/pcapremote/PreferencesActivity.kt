package com.pcapremote

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException


class PreferencesFragment :
        PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var preferenceSshServerPort: EditTextPreference? = null
    private var preferenceStartStopLogcatCapturing: Preference? = null
    private var aboutClickCount = 0

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        aboutClickCount = 0
        firebaseAnalytics = FirebaseAnalytics.getInstance(requireActivity())

        findPreference<Preference>(
                getString(R.string.preferences_keys_install_certificate))?.setOnPreferenceClickListener {

            startActivityForResult(
                    SslCerts.createInstallMitmCertIntent(requireActivity()),
                    MainActivity.INSTALL_CERTIFICATE_REQUEST_CODE)

            true
        }

        findPreference<Preference>(
                getString(R.string.preferences_keys_export_certificate))?.setOnPreferenceClickListener {

            startCreateDocumentActivity("mitm_key.pem", EXPORT_CERTIFICATE_REQUEST_CODE)
            true
        }

        preferenceSshServerPort = findPreference(getString(R.string.preferences_keys_ssh_server_port))
        preferenceSshServerPort?.setOnPreferenceChangeListener { _, newValue ->
            val newValueStr = newValue.toString()
            var result = true

            if (newValueStr.isEmpty()) {
                MiscUtils.renderErrorDialog(requireActivity(), getString(R.string.preferences_port_null_error))
                result = false
            }

            try {
                val newValueInt = newValueStr.toInt()
                if (result && 1024 >= newValueInt) {
                    MiscUtils.renderErrorDialog(requireActivity(), getString(R.string.preferences_privileged_port_error))
                    result = false
                }
            } catch (ex: NumberFormatException) {
                MiscUtils.renderErrorDialog(requireActivity(), getString(R.string.preferences_invalid_format))
                result = false
            }

            result
        }

        preferenceSshServerPort?.summary = Preferences.sshPort.toString()

        findPreference<Preference>(
                getString(R.string.preferences_keys_export_ssh_server_certificate))?.setOnPreferenceClickListener {

            MiscUtils.analyticsLogSelectContent(firebaseAnalytics, it.key, null)
            startCreateDocumentActivity("ssh_key.pem", EXPORT_SSH_CERTIFICATE_REQUEST_CODE)
            true
        }

        findPreference<Preference>(
                getString(R.string.preferences_keys_about_privacy_policy))?.setOnPreferenceClickListener {

            MiscUtils.startPrivacyPolicyActivity(requireActivity())
            true
        }

        findPreference<Preference>(
                getString(R.string.preferences_keys_about_open_source_licenses))?.setOnPreferenceClickListener {

            startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            true
        }

        val aboutPref = findPreference<Preference>(getString(R.string.preferences_keys_about_build_info))
        aboutPref?.summary = MiscUtils.appVersion(requireActivity())

        preferenceStartStopLogcatCapturing = findPreference(
                getString(R.string.preferences_keys_debug_start_stop_logcat_capturing))

        aboutPref?.setOnPreferenceClickListener {
            aboutClickCount++
            if (aboutClickCount >= 5) {
                Application.instance.enableDebugMode()

                preferenceStartStopLogcatCapturing?.isVisible = true

                Toast.makeText(
                        requireActivity(),
                        R.string.preferences_debug_mode_enabled,
                        Toast.LENGTH_LONG).show()
            }

            true
        }

        preferenceStartStopLogcatCapturing?.isVisible = Preferences.debugMode

        val updateStartStopLogcatCapturingPreferenceTitle = {
            preferenceStartStopLogcatCapturing?.title = if (Logcat.isRunning) {
                getString(R.string.preferences_debug_stop_logcat_capturing)
            } else {
                getString(R.string.preferences_debug_start_logcat_capturing)
            }
        }

        preferenceStartStopLogcatCapturing?.setOnPreferenceClickListener {
            if (Logcat.isRunning) {
                Logcat.stopCapturing()

                val file = Logcat.getFile(requireContext())
                if (file.exists()) {
                    startCreateDocumentActivity(file.name, EXPORT_LOGCAT_REQUEST_CODE)
                }
            } else {
                val toastStringResId = if (Logcat.startCapturing(requireContext())) {
                    R.string.preferences_debug_logcat_capturing_started
                } else {
                    R.string.preferences_debug_start_logcat_capturing_error
                }

                Toast.makeText(
                        requireActivity(),
                        toastStringResId,
                        Toast.LENGTH_LONG).show()
            }

            updateStartStopLogcatCapturingPreferenceTitle()
            true
        }

        val preferenceDarkTheme = findPreference<Preference>(getString(R.string.preferences_keys_dark_theme)) as SwitchPreference?
        preferenceDarkTheme?.isChecked = Preferences.darkTheme

        preferenceDarkTheme?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = (newValue as Boolean)
            Preferences.darkTheme = isEnabled
            MiscUtils.setDarkThemeEnabled(isEnabled)
            true
        }
    }

    private fun startCreateDocumentActivity(fileName: String, requestCode: Int) {
        try {
            val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
            exportIntent.type = "text/csv"
            exportIntent.putExtra(Intent.EXTRA_TITLE, fileName)
            startActivityForResult(exportIntent, requestCode)
        } catch (ex: Exception) {
            Timber.e(ex)
            ex.message?.let { Toast.makeText(requireActivity(), it, Toast.LENGTH_LONG).show() }
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == getString(R.string.preferences_keys_ssh_server_port)) {
            preferenceSshServerPort?.summary = Preferences.sshPort.toString()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Timber.i("onActivityResult: %d", requestCode)

        if (Activity.RESULT_OK != resultCode) {
            return
        }

        if (null == data) {
            return
        }

        val renderExportedToast = when (requestCode) {
            EXPORT_CERTIFICATE_REQUEST_CODE -> {
                saveCertificate(data, SslCerts.getMitmKey(requireActivity()))
            }

            EXPORT_SSH_CERTIFICATE_REQUEST_CODE -> {
                saveCertificate(data, SslCerts.getSshKey(requireActivity()))
            }

            EXPORT_LOGCAT_REQUEST_CODE -> {
                saveFile(data, Logcat.getFile(requireContext()))
            }
            else -> false
        }

        if (renderExportedToast) {
            Toast.makeText(
                    requireActivity(),
                    R.string.preferences_file_exported,
                    Toast.LENGTH_LONG).show()
        }
    }

    private fun saveCertificate(data: Intent, key: String): Boolean {
        try {
            data.data?.let { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { stream ->
                    if (null != stream) {
                        stream.write(key.toByteArray())
                        return true
                    }
                }
            }
        } catch (ex: IOException) {
            Timber.e(ex)
            ex.message?.let { Toast.makeText(requireActivity(), it, Toast.LENGTH_LONG).show() }
        }

        return false
    }

    private fun saveFile(data: Intent, file: File): Boolean {
        try {
            data.data?.let { uri ->
                requireActivity().contentResolver.openOutputStream(uri).use { outputStream ->
                    if (null != outputStream) {
                        FileInputStream(file).use { inputStream ->
                            MiscUtils.copyStream(inputStream, outputStream)
                            return true
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            Timber.e(ex)
            ex.message?.let { Toast.makeText(requireActivity(), it, Toast.LENGTH_LONG).show() }
        }

        return false
    }

    companion object {
        const val EXPORT_CERTIFICATE_REQUEST_CODE = 8
        const val EXPORT_SSH_CERTIFICATE_REQUEST_CODE = 9
        const val EXPORT_LOGCAT_REQUEST_CODE = 10
    }
}

class PreferencesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setTitle(R.string.preferences_title)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                    R.id.container, PreferencesFragment()).commit()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
