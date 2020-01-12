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