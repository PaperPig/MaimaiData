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
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.MmdPlayerRtsongDividerLayoutBinding
import com.paperpig.maimaidata.databinding.MmdPlayerRtsongLayoutBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.utils.WindowsUtils

class RecordAdapter(private val songData: List<SongData>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_RECORD = 0
        const val TYPE_DIVIDER = 1
    }

    private var versionType = 0
    private var isMatching = true

    private var originList = listOf<Record>()
    private var recordList = listOf<Record>()
        set(value) {
            field = value
                .filter {
                    val find = songData.find { data -> data.id == it.song_id }
                    if (find != null) {
                        if (versionType == 0) {
                            !find.basic_info.is_new
                        } else {
                            find.basic_info.is_new
                        }
                    } else {
                        isMatching = false
                        false
                    }
                }
                .sortedByDescending { it.ra }
        }


    inner class RecordHolder(binding: MmdPlayerRtsongLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val songLevel: TextView = binding.songLevel
        val songDiff: View = binding.songDiff
        val songJacket: ImageView = binding.songJacket
        val songJacketContainer: FrameLayout = binding.songJacketContainer
        val songTitle: TextView = binding.songTitle
        val songAcc: TextView = binding.songAcc
        val songRating: TextView = binding.songRating
        val songFsfsd: ImageView = binding.songFsfsd
        val songFcap: ImageView = binding.songFcap
        val songRank: ImageView = binding.songRank
        val songType: ImageView = binding.songType
        val out: FrameLayout = binding.outer
        val container: RelativeLayout = binding.container
    }

    inner class DividerHolder(binding: MmdPlayerRtsongDividerLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val dividerText: TextView = binding.dividerText
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_RECORD) {
            RecordHolder(
                MmdPlayerRtsongLayoutBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else {
            DividerHolder(
                MmdPlayerRtsongDividerLayoutBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }
    }

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        if (viewHolder is RecordHolder) {
            val context = viewHolder.itemView.context
            val record = recordList[getRealPosition(position)]
            viewHolder.songLevel.text = record.ds.toString()
            viewHolder.songTitle.text = record.title
            viewHolder.songAcc.text = String.format(
                context.getString(R.string.maimaidx_achievement_desc), record.achievements
            )
            viewHolder.songRating.text = String.format(
                context.getString(R.string.rating_scope), record.ra, (record.ds * 14.07).toInt()
            )

            val find = songData.find { it.id == record.song_id }
            if (find != null) {
                GlideApp.with(context)
                    .load(MaimaiDataClient.IMAGE_BASE_URL + find.basic_info.image_url)
                    .transition(DrawableTransitionOptions.withCrossFade()).apply(
                        RequestOptions.bitmapTransform(
                            RoundedCorners(
                                WindowsUtils.dp2px(
                                    context, 5f
                                ).toInt()
                            )
                        )
                    ).into(viewHolder.songJacket)
            }


            (viewHolder.out.background as GradientDrawable).setColor(
                ContextCompat.getColor(
                    context, record.getShadowColor()
                )
            )
            (viewHolder.container.background as GradientDrawable).setColor(
                ContextCompat.getColor(
                    context, record.getBackgroundColor()
                )
            )
            (viewHolder.songJacketContainer.background as GradientDrawable).setColor(
                ContextCompat.getColor(
                    context, record.getShadowColor()
                )
            )


            viewHolder.songFcap.setImageDrawable(
                ContextCompat.getDrawable(context, record.getFcIcon())
            )

            viewHolder.songFsfsd.setImageDrawable(
                ContextCompat.getDrawable(context, record.getFsIcon())
            )

            viewHolder.songRank.setImageDrawable(
                ContextCompat.getDrawable(context, record.getRankIcon())
            )

            viewHolder.songDiff.setBackgroundResource(
                record.getDifficultyDiff()
            )

            viewHolder.songType.setImageDrawable(
                ContextCompat.getDrawable(context, record.getTypeIcon())
            )
        } else if (viewHolder is DividerHolder) {
            if (versionType == 0) {
                viewHolder.dividerText.setText(R.string.old_version_divider)
            } else {
                viewHolder.dividerText.setText(R.string.new_version_divider)
            }

        }
    }

    override fun getItemCount(): Int {
        return if ((versionType == 0 && recordList.size > 25) || (versionType == 1 && recordList.size > 15)) {
            recordList.size + 1
        } else recordList.size
    }

    private fun getRealPosition(position: Int): Int {
        return if ((versionType == 0 && position > 25) || (versionType == 1 && position > 15)) {
            position - 1
        } else {
            position
        }
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

    fun setData(list: List<Record>, version: Int) {
        originList = list
        versionType = version
        recordList = originList
        notifyDataSetChanged()
    }

    fun getMatching(): Boolean = isMatching

}