package com.paperpig.maimaidata.ui.maimaidxprober

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AccountAdapter(
    private val accounts: List<String>,
    private val onItemClick: (String) -> Unit,
) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(android.R.id.text1)
        init {
            view.setOnClickListener {
                val account = accounts[getBindingAdapterPosition()]
                onItemClick(account)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = accounts.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = accounts[position]
    }
}
