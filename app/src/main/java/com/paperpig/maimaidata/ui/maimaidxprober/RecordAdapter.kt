package com.paperpig.maimaidata.ui.maimaidxprober

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.constat.Constant
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData

class RecordAdapter(private val songData: List<SongData>) :
    RecyclerView.Adapter<RecordAdapter.ViewHolder>() {

    private var versionType = 0

    private var originList = listOf<Record>()
    private var recordList = listOf<Record>()
        set(value) {
            field = value
                .filter {
                    if (versionType == 0) {
                        !it.is_new
                    } else {
                        it.is_new
                    }
                }
                .sortedByDescending { it.ra }
        }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songLevel: TextView = view.findViewById(R.id.song_level)
        val songDiff: View = view.findViewById(R.id.song_diff)
        val songJacket: ImageView = view.findViewById(R.id.song_jacket)
        val songJacketContainer: FrameLayout = view.findViewById(R.id.song_jacket_container)
        val songTitle: TextView = view.findViewById(R.id.song_title)
        val songAcc: TextView = view.findViewById(R.id.song_acc)
        val songRating: TextView = view.findViewById(R.id.song_rating)
        val songFsfsd: ImageView = view.findViewById(R.id.song_fsfsd)
        val songFcap: ImageView = view.findViewById(R.id.song_fcap)
        val songRank: ImageView = view.findViewById(R.id.song_rank)
        val songType: ImageView = view.findViewById(R.id.song_type)
        val out: FrameLayout = view.findViewById(R.id.outer)
        val container: RelativeLayout = view.findViewById(R.id.container)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mmd_player_rtsong_layout, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val context = holder.itemView.context
        val record = recordList[position]
        holder.songLevel.text = record.ds.toString()
        holder.songTitle.text = record.title
        holder.songAcc.text = String.format(context.getString(R.string.maimaidx_achievement_desc),
            record.achievements)
        holder.songRating.text = String.format(context.getString(R.string.rating_scope),
            record.ra,
            (record.ds * 14.07).toInt())


        songData.forEach {
            if (it.id == record.song_id) {
                Glide.with(context)
                    .load(Constant.IMAGE_BASE_URL + it.basic_info.image_url)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.songJacket)
                return@forEach
            }
        }

        (holder.out.background as GradientDrawable).setColor(ContextCompat.getColor(context,
            record.getShadowColor()))
        (holder.container.background as GradientDrawable).setColor(ContextCompat.getColor(context,
            record.getBackgroundColor()))
        (holder.songJacketContainer.background as GradientDrawable).setColor(ContextCompat.getColor(
            context,
            record.getShadowColor()))


        holder.songFcap.setImageDrawable(
            record.getFcIcon()?.let {
                ContextCompat.getDrawable(context, it)
            }
        )
        holder.songFsfsd.setImageDrawable(
            record.getFsIcon()?.let {
                ContextCompat.getDrawable(context, it)
            }
        )
        holder.songRank.setImageDrawable(
            ContextCompat.getDrawable(context, record.getRankIcon()))

        holder.songDiff.setBackgroundResource(
            record.getDifficultyDiff())

        holder.songType.setImageDrawable(
            ContextCompat.getDrawable(context, record.getTypeIcon()))
    }

    override fun getItemCount(): Int {
        return recordList.size
    }

    fun setData(list: List<Record>) {
        originList = list
        recordList = originList
        notifyDataSetChanged()
    }

    fun setVersion(int: Int) {
        versionType = int
        recordList = originList
        notifyDataSetChanged()
    }
}