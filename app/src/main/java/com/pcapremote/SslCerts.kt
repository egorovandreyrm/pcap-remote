package com.pcapremote

import android.content.Context
import android.content.Intent
import android.security.KeyChain
import java.io.File
import java.io.FileOutputStream


object SslCerts {
    private lateinit var mitmCert: String
    private lateinit var mitmKey: String
    private lateinit var sshKey: String

    fun createInstallMitmCertIntent(context: Context): Intent {
        val installIntent = KeyChain.createInstallIntent()
        installIntent.putExtra(KeyChain.EXTRA_CERTIFICATE, getMitmCert(context).toByteArray())
        installIntent.putExtra(KeyChain.EXTRA_NAME, "PCAP Remote")
        return installIntent
    }

    fun getMitmCert(context: Context): String {
        if (!::mitmCert.isInitialized) {
            mitmCert = MiscUtils.readFileFromAsset(context, "mitm_cert/cert.pem")
        }

        return mitmCert
    }

    fun getMitmKey(context: Context): String {
        if (!::mitmKey.isInitialized) {
            mitmKey = MiscUtils.readFileFromAsset(context, "mitm_cert/key.pem")
        }

        return mitmKey
    }

    fun getSshKey(context: Context): String {
        if (!::sshKey.isInitialized) {
            sshKey = MiscUtils.readFileFromAsset(context, "ssh_key.pem")
        }

        return sshKey
    }

    fun getSshKeyFile(context: Context): File {
        // sshsslkey.pem

        val file = File(context.filesDir, "ssh_key.pem")
        if (file.exists()) {
            return file
        }

        val key = getSshKey(context)
        FileOutputStream(file).use {
            it.write(key.toByteArray())
        }

        return file
    }
}