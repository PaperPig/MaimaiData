package com.paperpig.maimaidata.ui.songlist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.databinding.ItemDotsBinding

class DotsScrollAdapter(val context: Context, val res: Int) :
    RecyclerView.Adapter<DotsScrollAdapter.ViewHolder>() {
    inner class ViewHolder(binding: ItemDotsBinding) : RecyclerView.ViewHolder(binding.root) {
        val imageView: ImageView = binding.dotsImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemDotsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
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