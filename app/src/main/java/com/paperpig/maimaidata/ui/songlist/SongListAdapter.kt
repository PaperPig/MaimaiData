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
import com.paperpig.maimaidata.databinding.ItemCompactSongBinding
import com.paperpig.maimaidata.databinding.ItemCompactUtageSongBinding
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
        const val TYPE_COMPACT_NORMAL = 2
        const val TYPE_COMPACT_UTAGE = 3
    }

    var isCompactMode: Boolean = false
        private set

    fun setCompactMode(compact: Boolean) {
        if (isCompactMode != compact) {
            isCompactMode = compact
            if (itemCount > 0) {
                notifyItemRangeChanged(0, itemCount)
            }
        }
    }

    private var list = listOf<SongWithChartsEntity>()

    inner class NormalViewHolder(binding: ItemNormalSongBinding) :
        ViewHolder(binding.root) {
        val cardContainer: View = binding.cardContainer
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
        val cardContainer: View = binding.cardContainer
        val songJacket: ImageView = binding.songJacket
        val songTitle: TextView = binding.songTitle
        val songArtist: TextView = binding.songArtist
        val songGenre: TextView = binding.songGenre
        val songUtageKanji: TextView = binding.songUtageKanji
        val songLevelUtage: TextView = binding.songLevelUtage
        val songComment: TextView = binding.songComment
        val songUtagePartyMark: ImageView = binding.songUtagePartyMark
    }

    inner class CompactNormalViewHolder(binding: ItemCompactSongBinding) :
        ViewHolder(binding.root) {
        val songJacket: ImageView = binding.songJacket
        val songTitle: TextView = binding.songTitle
        val songArtist: TextView = binding.songArtist
        val difficultyBasic: TextView = binding.levelBasic
        val difficultyAdvanced: TextView = binding.levelAdvanced
        val difficultyExpert: TextView = binding.levelExpert
        val difficultyMaster: TextView = binding.levelMaster
        val difficultyRemaster: TextView = binding.levelRemaster
        val songType: ImageView = binding.songType
    }

    inner class CompactUtageViewHolder(binding: ItemCompactUtageSongBinding) :
        ViewHolder(binding.root) {
        val songJacket: ImageView = binding.songJacket
        val songTitle: TextView = binding.songTitle
        val songArtist: TextView = binding.songArtist
        val songUtageKanji: TextView = binding.songUtageKanji
        val songLevelUtage: TextView = binding.songLevelUtage
        val songComment: TextView = binding.songComment
        val songUtagePartyMark: ImageView = binding.songUtagePartyMark
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_NORMAL -> NormalViewHolder(
                ItemNormalSongBinding.inflate(inflater, parent, false)
            )

            TYPE_UTAGE -> UtageViewHolder(
                ItemUtageSongBinding.inflate(inflater, parent, false)
            )

            TYPE_COMPACT_NORMAL -> CompactNormalViewHolder(
                ItemCompactSongBinding.inflate(inflater, parent, false)
            )

            TYPE_COMPACT_UTAGE -> CompactUtageViewHolder(
                ItemCompactUtageSongBinding.inflate(inflater, parent, false)
            )

            else -> NormalViewHolder(
                ItemNormalSongBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemViewType(position: Int): Int {
        val isUtage = list[position].songData.genre == Constants.GENRE_UTAGE
        return if (isCompactMode) {
            if (isUtage) TYPE_COMPACT_UTAGE else TYPE_COMPACT_NORMAL
        } else {
            if (isUtage) TYPE_UTAGE else TYPE_NORMAL
        }
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

        val isCompact = holder is CompactNormalViewHolder || holder is CompactUtageViewHolder

        val cardView = when (holder) {
            is NormalViewHolder -> holder.cardContainer
            is UtageViewHolder -> holder.cardContainer
            else -> holder.itemView
        }

        val bgLayer =
            ((cardView.background as? LayerDrawable)?.getDrawable(0) as? LayerDrawable)
        val bgColor = bgLayer?.findDrawableByLayerId(R.id.song_list_bg) as? GradientDrawable
        val bgStroke = bgLayer?.findDrawableByLayerId(R.id.song_list_stroke) as? GradientDrawable
        val bgInnerStroke =
            bgLayer?.findDrawableByLayerId(R.id.song_list_inner_stroke) as? GradientDrawable

        bgColor?.setColor(ContextCompat.getColor(holder.itemView.context, songData.bgColor))
        bgInnerStroke?.setStroke(
            (if (isCompact) 1 else 3).toDp().toInt(),
            ContextCompat.getColor(holder.itemView.context, songData.bgColor)
        )
        bgStroke?.setStroke(
            (if (isCompact) 2 else 4).toDp().toInt(),
            ContextCompat.getColor(holder.itemView.context, songData.strokeColor)
        )

        when (holder) {
            is NormalViewHolder -> {
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
            }

            is UtageViewHolder -> {
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

            is CompactNormalViewHolder -> {
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
                    holder.difficultyRemaster.visibility = View.VISIBLE
                    holder.difficultyRemaster.text = charts[4].ds.toString()
                } else {
                    holder.difficultyRemaster.visibility = View.GONE
                }

                if (songData.type == Constants.CHART_TYPE_DX) {
                    holder.songType.setImageResource(R.drawable.ic_deluxe)
                } else {
                    holder.songType.setImageResource(R.drawable.ic_standard)
                }
            }

            is CompactUtageViewHolder -> {
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
    }

    fun setData(list: List<SongWithChartsEntity>) {
        this.list = list
        notifyDataSetChanged()
    }
}
