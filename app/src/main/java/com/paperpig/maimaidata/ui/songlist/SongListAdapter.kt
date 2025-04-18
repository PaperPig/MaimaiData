package com.paperpig.maimaidata.ui.songlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemNormalSongBinding
import com.paperpig.maimaidata.databinding.ItemUtageSongBinding
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.toDp


class SongListAdapter : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_UTAGE = 1
    }


    private var list = listOf<SongWithChartsEntity>()

    inner class NormalViewHolder(binding: ItemNormalSongBinding) :
        ViewHolder(binding.root) {
        val songJacket: ImageView = binding.songJacket
        val songTitle: TextView = binding.songTitle
        val songArtist: TextView = binding.songArtist
        val songGenre: TextView = binding.songGenre
        val difficultyBasic: TextView = binding.levelBasic
        val difficultyAdvanced: TextView = binding.levelAdvanced
        val difficultyExpert: TextView = binding.levelExpert
        val difficultyMaster: TextView = binding.levelMaster
        val difficultyRemaster: TextView = binding.levelRemaster
        val songType: ImageView = binding.songType
    }

    inner class UtageViewHolder(binding: ItemUtageSongBinding) :
        ViewHolder(binding.root) {
        val songJacket: ImageView = binding.songJacket
        val songTitle: TextView = binding.songTitle
        val songArtist: TextView = binding.songArtist
        val songGenre: TextView = binding.songGenre
        val songUtageKanji: TextView = binding.songUtageKanji
        val songLevelUtage: TextView = binding.songLevelUtage
        val songComment: TextView = binding.songComment
        val songUtagePartyMark = binding.songUtagePartyMark
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == TYPE_NORMAL) {
            NormalViewHolder(
                ItemNormalSongBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
            )
        } else UtageViewHolder(
            ItemUtageSongBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].songData.genre == Constants.GENRE_UTAGE) return TYPE_UTAGE else TYPE_NORMAL
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songData = list[position].songData
        val charts = list[position].charts
        holder.itemView.setOnClickListener {
            SongDetailActivity.actionStart(holder.itemView.context, list[position])
        }
        holder.itemView.setOnLongClickListener {
            val mClipData = ClipData.newPlainText("copyText", songData.title)
            (holder.itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                mClipData
            )
            Toast.makeText(
                holder.itemView.context,
                R.string.copy_song_name_success,
                Toast.LENGTH_SHORT
            ).show()
            true
        }


        val bgColor =
            ((holder.itemView.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_list_bg
            ) as GradientDrawable

        val bgStroke =
            ((holder.itemView.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_list_stroke
            ) as GradientDrawable

        val bgInnerStroke =
            ((holder.itemView.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_list_inner_stroke
            ) as GradientDrawable
        bgColor.setColor(ContextCompat.getColor(holder.itemView.context, songData.bgColor))
        bgInnerStroke.setStroke(
            3.toDp().toInt(), ContextCompat.getColor(
                holder.itemView.context,
                songData.bgColor
            )
        )

        bgStroke.setStroke(
            4.toDp().toInt(),
            ContextCompat.getColor(holder.itemView.context, songData.strokeColor)
        )

        if (holder is NormalViewHolder) {
            holder.songGenre.text = songData.genre
            val genreBg =
                ((holder.songGenre.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                    R.id.song_genre_bg
                ) as GradientDrawable
            genreBg.setColor(ContextCompat.getColor(holder.itemView.context, songData.bgColor))


            GlideApp.with(holder.itemView.context)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.songJacket)
            holder.songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    songData.strokeColor
                )
            )
            holder.songTitle.text = songData.title
            holder.songArtist.text = songData.artist
            holder.difficultyBasic.text = charts[0].ds.toString()
            holder.difficultyAdvanced.text = charts[1].ds.toString()
            holder.difficultyExpert.text = charts[2].ds.toString()
            holder.difficultyMaster.text = charts[3].ds.toString()
            if (charts.size == 5) {
                holder.difficultyRemaster.text = charts[4].ds.toString()
            } else {
                holder.difficultyRemaster.text = ""

            }

            if (songData.type == Constants.CHART_TYPE_DX) {
                holder.songType.setImageResource(R.drawable.ic_deluxe)
            } else {
                holder.songType.setImageResource(R.drawable.ic_standard)

            }
        } else if (holder is UtageViewHolder) {
            holder.songGenre.text = songData.genre
            val genreBg =
                ((holder.songGenre.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                    R.id.song_genre_bg
                ) as GradientDrawable
            genreBg.setColor(ContextCompat.getColor(holder.itemView.context, songData.bgColor))


            GlideApp.with(holder.itemView.context)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.songJacket)
            holder.songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    songData.strokeColor
                )
            )

            holder.songTitle.text = songData.title
            holder.songArtist.text = songData.artist
            holder.songLevelUtage.text = charts[0].level
            holder.songUtageKanji.text = songData.kanji
            holder.songComment.text = songData.comment
            holder.songUtagePartyMark.visibility =
                if (songData.buddy != null) View.VISIBLE else View.GONE


        }

    }


    fun setData(list: List<SongWithChartsEntity>) {
        this.list = list
        notifyDataSetChanged()
    }

}