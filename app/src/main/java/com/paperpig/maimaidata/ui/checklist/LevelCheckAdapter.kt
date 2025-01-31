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
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.DsSongData
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.toDp

class LevelCheckAdapter(
    val context: Context,
    private var songData: List<SongData>,     //歌曲信息列表
    private val recordList: List<Record>, //个人记录列表
    private var levelSelect: String   //指定难度
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //0为显示完成率标识，1为显示FC/AP标识，2为显示FDX标识
    private var displayMode = 0
    private var groupData: Map<Double, List<DsSongData>> = getFormatData()

    /**
     * 转换为adapter数据源
     */
    private fun getFormatData(): Map<Double, List<DsSongData>> {
        return songData.flatMap { songDatum ->
            songDatum.level.indices
                .filter { i -> songDatum.level[i] == levelSelect }
                .map { i ->
                    DsSongData(
                        songDatum.id,
                        songDatum.title,
                        songDatum.type,
                        songDatum.basic_info.image_url,
                        i,
                        songDatum.ds[i]
                    )
                }
        }.sortedByDescending { it.ds }.groupBy { it.ds }
    }

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

    inner class LevelViewHolder(binding: ItemLevelHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val levelTitle = binding.levelTitle
    }

    inner class ItemViewHolder(binding: ItemSongCheckBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songRecordMark = binding.songRecordMark
        val songType = binding.songType
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

            TYPE_LEVEL -> LevelViewHolder(
                ItemLevelHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )

            else -> ItemViewHolder(
                ItemSongCheckBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        }
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val format = context.getString(R.string.name_plate_achieved)

            val groupFlatten = groupData.values.flatten()
            val groupSize = groupData.values.sumOf { it.size }

            holder.tripleSCount.text = String.format(
                format, recordList.count {
                    it.achievements >= 100 && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.song_id }
                }, groupSize
            )

            holder.fcCount.text = String.format(
                format, recordList.count {
                    it.fc.isNotEmpty() && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.song_id }
                }, groupSize
            )

            holder.apCount.text = String.format(
                format, recordList.count {
                    (it.fc == "ap" || it.fc == "app") && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.song_id }
                }, groupSize
            )

            holder.fsdCount.text = String.format(
                format, recordList.count {
                    (it.fs == "fsd" || it.fs == "fsdp") && it.level == levelSelect && groupFlatten.any { songData -> songData.songId == it.song_id }
                }, groupSize
            )
        }
        if (holder is LevelViewHolder) {
            val data = getSongAt(position)
            holder.levelTitle.text = data.ds.toString()

        }
        if (holder is ItemViewHolder) {
            val data = getSongAt(position)
            holder.itemView.setOnClickListener {
                SongDetailActivity.actionStart(holder.itemView.context, data.songId)
            }


            holder.songJacket.apply {
                setBackgroundColor(
                    ContextCompat.getColor(
                        holder.itemView.context, getBorderColor(data.levelIndex)
                    )
                )
                GlideApp.with(holder.itemView.context)
                    .load(MaimaiDataClient.IMAGE_BASE_URL + data.imageUrl).into(holder.songJacket)
            }
            if (data.type == "DX") {
                GlideApp.with(holder.itemView.context).load(R.drawable.ic_deluxe)
                    .into(holder.songType)
            } else {
                GlideApp.with(holder.itemView.context).clear(holder.songType)
            }

            recordList.find { it.song_id == data.songId && it.level_index == data.levelIndex }
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

    fun updateDisplay() {
        displayMode = (displayMode + 1) % 3
        notifyDataSetChanged()
    }

    fun updateData(
        newLevelSelect: String
    ) {
        levelSelect = newLevelSelect
        groupData = getFormatData()
        notifyDataSetChanged()

    }


    override fun getItemCount(): Int {
        return groupData.size + groupData.values.sumOf { it.size } + 1
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

    private fun getSongAt(position: Int): DsSongData {
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

    private fun getBorderColor(levelIndex: Int): Int {
        return when (levelIndex) {
            0 -> R.color.basic
            1 -> R.color.advanced
            2 -> R.color.expert
            3 -> R.color.master
            4 -> R.color.remaster_border
            else -> 0
        }
    }
}