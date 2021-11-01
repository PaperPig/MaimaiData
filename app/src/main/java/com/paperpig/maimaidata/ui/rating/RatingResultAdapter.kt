package com.paperpig.maimaidata.ui.rating

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.databinding.ItemRatingReusltBinding
import com.paperpig.maimaidata.model.Rating

class RatingResultAdapter : RecyclerView.Adapter<RatingResultAdapter.ViewHolder>() {
    private var data = listOf<Rating>()

    inner class ViewHolder(binding: ItemRatingReusltBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val innerLevel: TextView = binding.innerLevel
        val achievement: TextView = binding.achievement
        val rating: TextView = binding.rating
        val totalRating: TextView = binding.totalRating
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemRatingReusltBinding.inflate(
                LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ratingModel = data[position]

        holder.innerLevel.text = ratingModel.innerLevel.toString()
        holder.achievement.text = ratingModel.achi
        holder.rating.text = ratingModel.rating.toString()
        holder.totalRating.text = ratingModel.total.toString()

    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setData(list: List<Rating>) {
        data = list
        notifyDataSetChanged()
    }
}