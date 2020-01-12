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

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.android.synthetic.main.activity_select_app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class SelectAppActivity : AppCompatActivity() {
    companion object {
        const val SELECTED_APP_EXTRA = "selectedApp"
    }

    data class App(
            val icon: Drawable,
            val appName: String,
            val packageName: String
    )

    private var listener = object : SelectAppRecyclerViewAdapter.Listener {
        override fun onAppSelected(packageName: String) {
            val listToSave = lastSelectedApps.toMutableList()

            listToSave.remove(packageName)

            while (listToSave.size >= 3) {
                listToSave.removeAt(0)
            }

            listToSave.add(packageName)
            Preferences.lastSelectedSingleAppsToCapture = listToSave

            val data = Intent()
            data.putExtra(SELECTED_APP_EXTRA, packageName)
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private val adapter = SelectAppRecyclerViewAdapter(listOf(), listener)
    private var items = listOf<App>()
    private var lastSelectedApps = Preferences.lastSelectedSingleAppsToCapture

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_app)

        supportActionBar?.let {
            it.setDisplayHomeAsUpEnabled(true)
            it.setDisplayShowHomeEnabled(true)
            it.title = getString(R.string.select_app_activity_title)
        }

        rvAppsList.adapter = adapter

        pbLoading.visibility = View.VISIBLE
        etSearchApp.visibility = View.GONE
        rvAppsList.visibility = View.GONE

        etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {

            }

            override fun beforeTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(text: CharSequence, p1: Int, p2: Int, p3: Int) {
                Timber.i("onTextChanged: $text")
                filterApps(text.toString())
            }
        })

        lifecycleScope.launch {
            items = withContext(Dispatchers.IO) {
                val list = mutableListOf<App>()
                val appInfos = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                for (appInfo in appInfos) {
                    if (appInfo.packageName == packageName) {
                        continue
                    }

                    if (lastSelectedApps.contains(appInfo.packageName)) {
                        continue
                    }

                    val app = App(
                            packageManager.getApplicationIcon(appInfo),
                            packageManager.getApplicationLabel(appInfo) as String,
                            appInfo.packageName)

                    list.add(app)
                }

                list.sortBy { it.appName }

                if (lastSelectedApps.isNotEmpty()) {
                    list.add(0, App(packageManager.getApplicationIcon(packageName), "", ""))
                }

                for (packageName in lastSelectedApps) {
                    try {
                        val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)

                        val app = App(
                                packageManager.getApplicationIcon(appInfo),
                                packageManager.getApplicationLabel(appInfo) as String,
                                packageName)

                        list.add(0, app)
                    } catch (ex: Exception) {
                        Timber.w(ex)
                    }
                }

                list
            }

            pbLoading.visibility = View.GONE
            etSearchApp.visibility = View.VISIBLE
            rvAppsList.visibility = View.VISIBLE

            adapter.items = items
            adapter.notifyDataSetChanged()
        }
    }

    private fun filterApps(filter: String) {
        if (filter.isNotEmpty()) {
            val newList = mutableListOf<App>()
            for (app in items) {
                if (app.appName.contains(filter, ignoreCase = true)) {
                    newList.add(app)
                }
            }

            adapter.items = newList
        } else {
            adapter.items = items
        }

        adapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
