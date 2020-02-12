package com.pcapremote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.fragment_how_to_faq.*
import kotlinx.android.synthetic.main.how_to_faq_card_view_item.view.*
import kotlinx.android.synthetic.main.log_item.view.*
import kotlinx.android.synthetic.main.log_item.view.tvTitle


class HowToFaqFragment : Fragment() {

    private data class Card(
            val title: String,
            val description: String?,
            val text: String?,
            val youtubeVideoLink: String?)

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private inner class Adapter(private val items: List<Card>)
        : RecyclerView.Adapter<ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(
                    R.layout.how_to_faq_card_view_item, parent, false)

            return ViewHolder(v)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.itemView.tvTitle.text = item.title

            if (null != item.description) {
                holder.itemView.tvDescription.visibility = View.VISIBLE
                holder.itemView.tvDescription.text = item.description
            } else {
                holder.itemView.tvDescription.visibility = View.GONE
            }
        }
    }

    private val cardItems = mutableListOf<Card>()

    init {
        cardItems.add(Card("Remote capturing in Wireshark", null,null, null))
        cardItems.add(Card("Capturing Options", "Detailed info on how they work",null, null))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_how_to_faq, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        rvHowToFaqCards.layoutManager = LinearLayoutManager(activity)
        rvHowToFaqCards.setHasFixedSize(true)
        rvHowToFaqCards.adapter = Adapter(cardItems)

    }
}