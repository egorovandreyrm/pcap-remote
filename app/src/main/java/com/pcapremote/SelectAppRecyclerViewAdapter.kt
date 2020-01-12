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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.app_list_item.view.*

class SelectAppRecyclerViewAdapter(
        var items: List<SelectAppActivity.App>,
        private val listener: Listener)

    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val APP_ITEM = 1
        private const val DELIMITER_ITEM = 2
    }

    interface Listener {
        fun onAppSelected(packageName: String)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].packageName.isNotEmpty()) APP_ITEM else DELIMITER_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = if(viewType == APP_ITEM) {
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_list_item, parent, false)
        } else {
            LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_list_delimiter, parent, false)
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (item.packageName.isEmpty()) {
            return
        }

        val img = item.icon
        holder.itemView.ivAppIcon.setImageDrawable(img)
        holder.itemView.tvAppName.text = item.appName
        holder.itemView.setOnClickListener {
            listener.onAppSelected(item.packageName)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
