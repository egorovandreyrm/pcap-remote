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
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.*
import android.view.KeyEvent.KEYCODE_BACK
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_webview.*
import timber.log.Timber

class WebViewActivity : AppCompatActivity() {

    companion object {
        const val URL_EXTRA = "url"
        const val TITLE_EXTRA = "title"
        const val SHARED_INFO_TEMPLATE_EXTRA = "shared_info_template"
    }

    private val webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            pbLoading.visibility = View.GONE
            wvInfoContent.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            setContentView(R.layout.activity_webview)

            supportActionBar?.let {
                it.setDisplayHomeAsUpEnabled(true)
                it.setDisplayShowHomeEnabled(true)

                intent.extras?.getString(TITLE_EXTRA)?.let { title ->
                    it.title = title
                }
            }

            wvInfoContent.webViewClient = webViewClient
            reload()
        } catch (ex: InflateException) {
            Timber.e(ex)
            renderWebViewErrorDialog()
        }
    }

    private fun renderWebViewErrorDialog() {
        val spannableStr = SpannableString(
                String.format(getString(R.string.webview_inflation_error), getUrlToLoad()))

        Linkify.addLinks(spannableStr, Linkify.WEB_URLS)

        val dialog = AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle(R.string.error_dialog_title)
                .setMessage(spannableStr)
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener {
                    finish()
                }
                .create()

        dialog.show()
        (dialog.findViewById(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
    }

    private fun reload() {
        getUrlToLoad()?.let {
            pbLoading.visibility = View.VISIBLE
            wvInfoContent.visibility = View.GONE
            wvInfoContent.loadUrl(getUrlToLoad())
        }
    }

    private fun getUrlToLoad(): String? {
        return intent.getStringExtra(URL_EXTRA)
    }

    private fun getSharedInfoTemplate(): String? {
        return intent.getStringExtra(SHARED_INFO_TEMPLATE_EXTRA)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.action_reload -> reload()
            R.id.action_share -> shareUrl()
            //R.id.action_rate_app -> rateApp()
            else -> return super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KEYCODE_BACK -> {
                    if (wvInfoContent.canGoBack()) {
                        wvInfoContent.goBack()
                    } else {
                        finish()
                    }
                    return true
                }
            }

        }
        return super.onKeyDown(keyCode, event)
    }

    private fun shareUrl() {
        getUrlToLoad()?.let {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"

            val template = getSharedInfoTemplate()
            val info = if (null != template) {
                String.format(template, it)
            } else {
                it
            }

            sharingIntent.putExtra(Intent.EXTRA_TEXT, info)

            startActivity(Intent.createChooser(
                    sharingIntent, getString(R.string.webview_activity_share_using)))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_info_menu, menu)
        return true
    }

}
