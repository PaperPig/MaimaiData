package com.paperpig.maimaidata.ui.maimaidxprober

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData

class ProberVersionAdapter(private val songList: List<SongData>) :
    RecyclerView.Adapter<ProberVersionAdapter.ViewHolder>() {
    private var recordList = listOf<Record>()

    class ViewHolder(val recyclerView: RecyclerView) : RecyclerView.ViewHolder(recyclerView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val recyclerView = RecyclerView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        return ViewHolder(recyclerView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.recyclerView.apply {
            val recordAdapter = RecordAdapter(songList)
            adapter = recordAdapter
            recordAdapter.setData(recordList,position)
            layoutManager = LinearLayoutManager(holder.itemView.context)

        }
    }

    override fun getItemCount(): Int {
        return 2
    }

    fun setData(data: List<Record>) {
        recordList = data
        notifyDataSetChanged()
    }


}