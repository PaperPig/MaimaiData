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
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.toDp

class RecordAdapter(private val dataList: List<SongWithChartsEntity>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_RECORD = 0
        const val TYPE_DIVIDER = 1
    }

    private var versionType = 0
    private var _isMatching = true
    val isMatching: Boolean
        get() = _isMatching


    private var originList = listOf<RecordEntity>()
    private var recordList = listOf<RecordEntity>()
        set(value) {
            field = value
                .filter {
                    val find = dataList.find { data -> data.songData.id == it.songId }
                    if (find != null) {
                        if (versionType == 0) {
                            !find.songData.isNew
                        } else {
                            find.songData.isNew
                        }
                    } else {
                        _isMatching = false
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
                context.getString(R.string.rating_scope), ConvertUtils.achievementToRating(
                    (record.ds * 10).toInt(),
                    (record.achievements * 10000).toInt()
                ), (record.ds * 22.512).toInt()
            )

            dataList.find { it.songData.id == record.songId }?.let { data ->
                viewHolder.itemView.setOnClickListener {
                    SongDetailActivity.actionStart(viewHolder.itemView.context, data)
                }
                GlideApp.with(context)
                    .load(MaimaiDataClient.IMAGE_BASE_URL + data.songData.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade()).apply(
                        RequestOptions.bitmapTransform(
                            RoundedCorners(
                                5.toDp().toInt(),
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
        return if ((versionType == 0 && recordList.size > 35) || (versionType == 1 && recordList.size > 15)) {
            recordList.size + 1
        } else recordList.size
    }

    private fun getRealPosition(position: Int): Int {
        return if ((versionType == 0 && position > 35) || (versionType == 1 && position > 15)) {
            position - 1
        } else {
            position
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (versionType == 0) {
            if (position == 35) {
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

    fun setData(list: List<RecordEntity>, version: Int) {
        originList = list
        versionType = version
        recordList = originList
        notifyDataSetChanged()
    }

}