package com.paperpig.maimaidata.ui.rating

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Rating

class RatingResultAdapter : RecyclerView.Adapter<RatingResultAdapter.ViewHolder>() {
    private var data = listOf<Rating>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val innerLevel: TextView = view.findViewById(R.id.innerLevel)
        val achievement: TextView = view.findViewById(R.id.achievement)
        val rating: TextView = view.findViewById(R.id.rating)
        val totalRating: TextView = view.findViewById(R.id.totalRating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_rating_reuslt, parent, false)
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