package com.paperpig.maimaidata.ui.checklist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemCheckHeaderBinding
import com.paperpig.maimaidata.databinding.ItemLevelHeaderBinding
import com.paperpig.maimaidata.databinding.ItemSongCheckBinding
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class VersionCheckAdapter(
    val context: Context,
    private var dataList: List<SongWithChartsEntity>,
    private val recordList: List<RecordEntity>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //0为显示完成率标识，1为显示FC/AP标识，2为显示FDX标识
    private var displayMode = 0
    private var groupData = dataList.groupBy { it.charts[3].level }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_LEVEL = 1
        const val TYPE_NORMAL = 2
    }

    inner class HeaderViewHolder(binding: ItemCheckHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tripleSCount = binding.tripleSCount
        val fcCount = binding.fcCount
        val apCount = binding.apCount
        val fsdCount = binding.fsdCount
    }

    inner class LevelHolder(binding: ItemLevelHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val levelTitle = binding.levelTitle
    }

    inner class ViewHolder(binding: ItemSongCheckBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songRecordMark = binding.songRecordMark
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                ItemCheckHeaderBinding.inflate(
                    LayoutInflater.from(
                        context
                    ), parent, false
                )
            )

            TYPE_LEVEL -> LevelHolder(
                ItemLevelHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> ViewHolder(
                ItemSongCheckBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val format = context.getString(R.string.name_plate_achieved)

            holder.tripleSCount.text = String.format(
                format, recordList.count {
                    it.achievements >= 100 && dataList.any { data -> data.songData.id == it.songId }
                }, dataList.size
            )

            holder.fcCount.text = String.format(
                format, recordList.count {
                    it.fc.isNotEmpty() && dataList.any { data -> data.songData.id == it.songId }
                }, dataList.size
            )

            holder.apCount.text = String.format(
                format, recordList.count {
                    (it.fc == "ap" || it.fc == "app") && dataList.any { data -> data.songData.id == it.songId }
                }, dataList.size
            )

            holder.fsdCount.text = String.format(
                format, recordList.count {
                    (it.fs == "fsd" || it.fs == "fsdp") && dataList.any { data -> data.songData.id == it.songId }
                }, dataList.size
            )
        }
        if (holder is LevelHolder) {
            val data = getSongAt(position)
            holder.levelTitle.text = "Level " + data.charts[3].level

        }
        if (holder is ViewHolder) {
            val data = getSongAt(position)
            holder.itemView.setOnClickListener {
                SongDetailActivity.actionStart(holder.itemView.context, data)
            }
            holder.songJacket.apply {
                setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context, R.color.master
                    )
                )
                GlideApp.with(holder.itemView.context)
                    .load(MaimaiDataClient.IMAGE_BASE_URL + data.songData.imageUrl)
                    .into(this)
            }


            recordList.find { it.songId == data.songData.id }
                ?.let { record ->
                    holder.songJacket.colorFilter =
                        PorterDuffColorFilter(
                            Color.argb(128, 128, 128, 128),
                            PorterDuff.Mode.SRC_ATOP
                        )
                    when (displayMode) {
                        0 -> {
                            GlideApp.with(holder.itemView.context).load(record.getRankIcon())
                                .override(
                                    50.toDp().toInt(),
                                    22.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        1 -> {
                            GlideApp.with(holder.itemView.context).load(record.getFcIcon())
                                .override(
                                    30.toDp().toInt(),
                                    30.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        2 -> {
                            GlideApp.with(holder.itemView.context).load(record.getFsIcon())
                                .override(
                                    30.toDp().toInt(),
                                    30.toDp().toInt()
                                )
                                .into(holder.songRecordMark)
                        }

                        else -> {}
                    }
                } ?: run {
                holder.songJacket.colorFilter = null
                holder.songRecordMark.setImageDrawable(null)
            }
        }
    }


    override fun getItemCount(): Int {
        return groupData.size + dataList.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_HEADER
        var count = 0
        for (groupDatum in groupData) {
            val size = groupDatum.value.size + 1
            if (position - 1 < count + size) {
                return if (position - 1 == count) {
                    TYPE_LEVEL
                } else TYPE_NORMAL
            }
            count += size
        }
        throw IllegalArgumentException()
    }

    fun updateDisplay() {
        displayMode = (displayMode + 1) % 3
        notifyDataSetChanged()
    }

    fun updateData(
        newSongData: List<SongWithChartsEntity>,
    ) {
        dataList = newSongData
        groupData = dataList.groupBy { it.charts[3].level }
        notifyDataSetChanged()

    }


    private fun getSongAt(position: Int): SongWithChartsEntity {
        var count = 0
        for (groupDatum in groupData) {
            val size = groupDatum.value.size + 1
            if (position - 1 < count + size) {
                return if (position - 1 == count) {
                    groupDatum.value[0]
                } else groupDatum.value[position - count - 1 - 1]
            }
            count += size
        }
        throw IllegalArgumentException()
    }

}