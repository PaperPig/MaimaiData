package com.paperpig.maimaidata.ui.maimaidxprober

import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity

class ProberVersionAdapter(dataList: List<SongWithChartsEntity>) :
    RecyclerView.Adapter<ProberVersionAdapter.ViewHolder>() {
    private var recordList = listOf<RecordEntity>()
    private var isDataMatching = true
    private var b35Adapter: RecordAdapter = RecordAdapter(dataList)
    private var b15Adapter: RecordAdapter = RecordAdapter(dataList)

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
            adapter = if (position == 0) {
                b35Adapter
            } else b15Adapter
            layoutManager = LinearLayoutManager(holder.itemView.context)

        }
    }

    override fun getItemCount(): Int {
        return 2
    }

    fun setData(data: List<RecordEntity>) {
        recordList = data
        b35Adapter.setData(recordList, 0)
        b15Adapter.setData(recordList, 1)
        isDataMatching = b35Adapter.isMatching && b15Adapter.isMatching
        notifyDataSetChanged()
    }

    fun isDataMatching(): Boolean = isDataMatching
}