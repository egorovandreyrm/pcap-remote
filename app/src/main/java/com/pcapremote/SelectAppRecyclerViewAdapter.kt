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
