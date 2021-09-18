package com.paperpig.maimaidata.ui.songlist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R

class DotsScrollAdapter(val context: Context, val res: Int) :
    RecyclerView.Adapter<DotsScrollAdapter.ViewHolder>() {
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.dotsImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dots, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.imageView.setImageDrawable(
            ContextCompat.getDrawable(context, res)
        )
    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }
}