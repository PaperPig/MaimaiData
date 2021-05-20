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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.constat.Constant
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.WindowsUtils

class RecordAdapter(private val songData: List<SongData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_RECORD = 0
        const val TYPE_DIVIDER = 1
    }

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


    inner class RecordHolder(view: View) : RecyclerView.ViewHolder(view) {
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

    inner class DividerHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dividerText: TextView = view.findViewById(R.id.dividerText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_RECORD) {
            RecordHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.mmd_player_rtsong_layout, parent, false)
            )
        } else {
            DividerHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.mmd_player_rtsong_divider_layout, parent, false)
            )
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is RecordHolder) {
            val context = viewHolder.itemView.context
            val record = recordList[position]
            viewHolder.songLevel.text = record.ds.toString()
            viewHolder.songTitle.text = record.title
            viewHolder.songAcc.text =
                String.format(context.getString(R.string.maimaidx_achievement_desc),
                    record.achievements)
            viewHolder.songRating.text = String.format(context.getString(R.string.rating_scope),
                record.ra,
                (record.ds * 14.07).toInt())


            songData.forEach {
                if (it.id == record.song_id) {
                    Glide.with(context)
                        .load(Constant.IMAGE_BASE_URL + it.basic_info.image_url)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .apply(RequestOptions.bitmapTransform(RoundedCorners(WindowsUtils.dp2px(
                            context,
                            5f).toInt())))
                        .into(viewHolder.songJacket)
                    return@forEach
                }
            }

            (viewHolder.out.background as GradientDrawable).setColor(ContextCompat.getColor(
                context,
                record.getShadowColor()))
            (viewHolder.container.background as GradientDrawable).setColor(ContextCompat.getColor(
                context,
                record.getBackgroundColor()))
            (viewHolder.songJacketContainer.background as GradientDrawable).setColor(ContextCompat.getColor(
                context,
                record.getShadowColor()))


            viewHolder.songFcap.setImageDrawable(
                ContextCompat.getDrawable(context, record.getFcIcon()))

            viewHolder.songFsfsd.setImageDrawable(
                ContextCompat.getDrawable(context, record.getFsIcon()))

            viewHolder.songRank.setImageDrawable(
                ContextCompat.getDrawable(context, record.getRankIcon()))

            viewHolder.songDiff.setBackgroundResource(
                record.getDifficultyDiff())

            viewHolder.songType.setImageDrawable(
                ContextCompat.getDrawable(context, record.getTypeIcon()))
        } else if (viewHolder is DividerHolder) {
            if (versionType == 0) {
                viewHolder.dividerText.setText(R.string.old_version_divider)
            } else {
                viewHolder.dividerText.setText(R.string.new_version_divider)
            }

        }
    }

    override fun getItemCount(): Int {
        return recordList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (versionType == 0) {
            if (position == 25) {
                TYPE_DIVIDER
            } else {
                TYPE_RECORD
            }
        } else {
            if (position == 15) {
                TYPE_DIVIDER
            } else {
                TYPE_RECORD
            }

        }
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