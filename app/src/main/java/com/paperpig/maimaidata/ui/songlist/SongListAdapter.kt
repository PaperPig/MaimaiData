package com.paperpig.maimaidata.ui.songlist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.text.TextUtils
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
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemNormalSongBinding
import com.paperpig.maimaidata.databinding.ItemUtageSongBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.toDp
import com.paperpig.maimaidata.utils.versionCheck
import java.util.Locale


class SongListAdapter : RecyclerView.Adapter<ViewHolder>() {
    private val spUtils = SharePreferencesUtils(MaimaiDataApplication.instance, "songInfo")
    private val remasterAscComparator: java.util.Comparator<SongData> = Comparator { a1, a2 ->

        if (a1.ds.size < 5 && a2.ds.size < 5) {
            0
        } else if (a1.ds.size < 5)
            1
        else if (a2.ds.size < 5) {
            -1
        } else a1.ds[4].compareTo(a2.ds[4])
    }

    private val remasterDescComparator: java.util.Comparator<SongData> = Comparator { a1, a2 ->

        if (a1.ds.size < 5 && a2.ds.size < 5) {
            0
        } else if (a1.ds.size < 5)
            1
        else if (a2.ds.size < 5) {
            -1
        } else a2.ds[4].compareTo(a1.ds[4])
    }

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_UTAGE = 1
    }


    private var isLevelBySort = false
    private var selectLevel = ""
    private var selectDifficulty = "DEFAULT"
    private var searchText = ""
    private var isShowFavoriteSong = false
    private var originList = listOf<SongData>()
    private var versionList = listOf<String>()
    private var sortList = listOf<String>()

    private var songList = listOf<SongData>()
        set(value) {
            originList = value
            field = value.filter {
                if (searchText.isEmpty() || TextUtils.isEmpty(it.title)) {
                    return@filter true
                }
                return@filter it.title.lowercase(Locale.ROOT)
                    .contains(searchText.lowercase(Locale.ROOT))
            }.filter {
                if (sortList.isNotEmpty()) {
                    return@filter sortList.contains(it.basic_info.genre)
                } else
                    return@filter true
            }.filter {
                if (versionList.isNotEmpty()) {
                    return@filter versionList.versionCheck(it.basic_info.from)
                } else return@filter true
            }.filter {
                if (isShowFavoriteSong) {
                    spUtils.isFavorite(it.id)
                } else {
                    true
                }
            }
            if (isLevelBySort) {
                field = field.filter {
                    it.level.contains(selectLevel)
                }
            }
            if (selectDifficulty != "DEFAULT") {
                when (selectDifficulty) {
                    "EXPERT-升序" -> field =
                        field.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                            .sortedBy { it.ds[2] }

                    "EXPERT-降序" -> field =
                        field.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                            .sortedByDescending { it.ds[2] }

                    "MASTER-升序" -> field =
                        field.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                            .sortedBy { it.ds[3] }

                    "MASTER-降序" -> field =
                        field.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                            .sortedByDescending { it.ds[3] }

                    "RE:MASTER-升序" -> field =
                        field.sortedWith(remasterAscComparator)
                            .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }

                    "RE:MASTER-降序" -> field =
                        field.sortedWith(remasterDescComparator)
                            .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }

                    else -> return
                }
            }


        }


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
        return songList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (songList[position].basic_info.genre == "宴会場") return TYPE_UTAGE else TYPE_NORMAL
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val songData = songList[position]
        holder.itemView.setOnClickListener {
            SongDetailActivity.actionStart(holder.itemView.context, songData)
        }
        holder.itemView.setOnLongClickListener {
            val mClipData = ClipData.newPlainText("copyText", songData.basic_info.title)
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
        bgColor.setColor(ContextCompat.getColor(holder.itemView.context, songData.getBgColor()))
        bgInnerStroke.setStroke(
            3.toDp().toInt(), ContextCompat.getColor(
                holder.itemView.context,
                songData.getBgColor()
            )
        )

        bgStroke.setStroke(
            4.toDp().toInt(),
            ContextCompat.getColor(holder.itemView.context, songData.getStrokeColor())
        )

        if (holder is NormalViewHolder) {
            holder.songGenre.text = songData.basic_info.genre
            val genreBg =
                ((holder.songGenre.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                    R.id.song_genre_bg
                ) as GradientDrawable
            genreBg.setColor(ContextCompat.getColor(holder.itemView.context, songData.getBgColor()))


            GlideApp.with(holder.itemView.context)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.basic_info.image_url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.songJacket)
            holder.songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    songData.getStrokeColor()
                )
            )
            holder.songTitle.text = songData.basic_info.title
            holder.songArtist.text = songData.basic_info.artist
            holder.difficultyBasic.text = songData.ds[0].toString()
            holder.difficultyAdvanced.text = songData.ds[1].toString()
            holder.difficultyExpert.text = songData.ds[2].toString()
            holder.difficultyMaster.text = songData.ds[3].toString()
            if (songData.ds.size == 5) {
                holder.difficultyRemaster.text = songData.ds[4].toString()
            } else {
                holder.difficultyRemaster.text = ""

            }

            if (songData.type == "DX") {
                holder.songType.setImageResource(R.drawable.ic_deluxe)
            } else {
                holder.songType.setImageResource(R.drawable.ic_standard)

            }
        } else if (holder is UtageViewHolder) {
            holder.songGenre.text = songData.basic_info.genre
            val genreBg =
                ((holder.songGenre.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                    R.id.song_genre_bg
                ) as GradientDrawable
            genreBg.setColor(ContextCompat.getColor(holder.itemView.context, songData.getBgColor()))


            GlideApp.with(holder.itemView.context)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.basic_info.image_url)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.songJacket)
            holder.songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    songData.getStrokeColor()
                )
            )

            holder.songTitle.text = songData.basic_info.title
            holder.songArtist.text = songData.basic_info.artist
            holder.songLevelUtage.text = songData.level[0]
            holder.songUtageKanji.text = songData.basic_info.kanji
            holder.songComment.text = songData.basic_info.comment
            holder.songUtagePartyMark.visibility =
                if (songData.basic_info.buddy != null) View.VISIBLE else View.GONE


        }

    }


    fun search(
        str: String,
        list1: List<String>,
        list2: List<String>,
        level: String,
        sequencing: String,
        isShowFavor: Boolean
    ) {
        searchText = str
        sortList = list1
        versionList = list2
        if (level == "ALL") isLevelBySort = false
        else {
            isLevelBySort = true
            selectLevel = ConvertUtils.getLevel(level)
        }
        isShowFavoriteSong = isShowFavor
        selectDifficulty = sequencing
        songList = originList
        notifyDataSetChanged()
    }

    fun setData(list: List<SongData>) {
        songList = list
        notifyDataSetChanged()
    }

}