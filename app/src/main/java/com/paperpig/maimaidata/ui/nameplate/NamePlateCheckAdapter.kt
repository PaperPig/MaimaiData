package com.paperpig.maimaidata.ui.nameplate

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemNamePlateCheckBinding
import com.paperpig.maimaidata.databinding.ItemNamePlateHeaderBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient

class NamePlateCheckAdapter(
    private val songData: List<SongData>,
    private val record: List<Record>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_NORMAL = 1
    }


    inner class HeaderHolder(binding: ItemNamePlateHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val tripleSCount = binding.tripleSCount
        val fcCount = binding.fcCount
        val apCount = binding.apCount
        val fsdCount = binding.fsdCount
    }

    inner class ViewHolder(binding: ItemNamePlateCheckBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val songJacket = binding.songJacket
        val songTitle = binding.songTitle
        val songRank = binding.songRank
        val songFcap = binding.songFcap
        val songFsFsd = binding.songFsfsd
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) HeaderHolder(
            ItemNamePlateHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        ) else
            ViewHolder(
                ItemNamePlateCheckBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderHolder) {
            val format = holder.itemView.context.getString(R.string.name_plate_achieved)

            holder.tripleSCount.text =
                String.format(
                    format,
                    record.count {
                        it.achievements >= 100 && songData.any { songData -> songData.id == it.song_id }
                    },
                    songData.size
                )

            holder.fcCount.text = String.format(
                format,
                record.count {
                    it.fc.isNotEmpty() && songData.any { songData -> songData.id == it.song_id }
                },
                songData.size
            )

            holder.apCount.text = String.format(
                format, record.count {
                    (it.fc == "ap" || it.fc == "app") && songData.any { songData -> songData.id == it.song_id }
                }, songData.size
            )

            holder.fsdCount.text = String.format(
                format, record.count {
                    (it.fs == "fsd" || it.fs == "fsdp") && songData.any { songData -> songData.id == it.song_id }
                }, songData.size
            )

        }
        if (holder is ViewHolder) {
            val data = songData[position - 1]
            GlideApp.with(holder.itemView.context)
                .load(MaimaiDataClient.IMAGE_BASE_URL + data.basic_info.image_url)
                .into(holder.songJacket)
            holder.songTitle.text = data.title
            val find = record.find { it.song_id == data.id }
            if (find != null) {
                GlideApp.with(holder.itemView.context).load(find.getRankIcon())
                    .into(holder.songRank)
                GlideApp.with(holder.itemView.context).load(find.getFcIcon()).into(holder.songFcap)
                GlideApp.with(holder.itemView.context).load(find.getFsIcon()).into(holder.songFsFsd)
            } else {
                holder.songRank.setImageDrawable(null)
                GlideApp.with(holder.itemView.context).load(R.drawable.mmd_player_rtsong_stub)
                    .into(holder.songFcap)
                GlideApp.with(holder.itemView.context).load(R.drawable.mmd_player_rtsong_stub)
                    .into(holder.songFsFsd)
            }
        }
    }

    override fun getItemCount(): Int {
        return songData.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_HEADER
        else TYPE_NORMAL
    }


}