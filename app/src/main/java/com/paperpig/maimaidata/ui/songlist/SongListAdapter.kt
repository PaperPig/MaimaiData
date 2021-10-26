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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.ui.songdetail.SongDetailActivity
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.WindowsUtils
import com.paperpig.maimaidata.utils.versionCheck
import java.util.*
import kotlin.Comparator


class SongListAdapter : RecyclerView.Adapter<SongListAdapter.ViewHolder>() {
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
                return@filter it.title.toLowerCase(Locale.ROOT)
                    .contains(searchText.toLowerCase(Locale.ROOT))
            }.filter {
                if (sortList.isNotEmpty()) {
                    return@filter sortList.contains(it.basic_info.genre)
                } else
                    return@filter true
            }.filter {
                if (versionList.isNotEmpty()) {
                    if (versionList.contains("でらっくす 2021")) {
                        return@filter versionList.versionCheck(it.basic_info.from) || it.basic_info.is_new
                    } else return@filter versionList.versionCheck(it.basic_info.from) && !it.basic_info.is_new
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
                        field.sortedBy { it.ds[2] }
                            .filter { if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                    "EXPERT-降序" -> field =
                        field.sortedByDescending { it.ds[2] }
                            .filter { if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                    "MASTER-升序" -> field =
                        field.sortedBy { it.ds[3] }
                            .filter { if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                    "MASTER-降序" -> field =
                        field.sortedByDescending { it.ds[3] }
                            .filter { if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                    "RE:MASTER-升序" -> field =
                        field.sortedWith(remasterAscComparator).sortedWith(
                            remasterAscComparator
                        )
                            .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }
                    "RE:MASTER-降序" -> field =
                        field.sortedWith(remasterDescComparator).sortedWith(
                            remasterDescComparator
                        )
                            .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }
                    else -> return
                }
            }


        }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val songJacket: ImageView = view.findViewById(R.id.songJacket)
        val songTitle: TextView = view.findViewById(R.id.songTitle)
        val songArtist: TextView = view.findViewById(R.id.songArtist)
        val songGenre: TextView = view.findViewById(R.id.songGenre)
        val difficultyBasic: TextView = view.findViewById(R.id.levelBasic)
        val difficultyAdvanced: TextView = view.findViewById(R.id.levelAdvanced)
        val difficultyExpert: TextView = view.findViewById(R.id.levelExpert)
        val difficultyMaster: TextView = view.findViewById(R.id.levelMaster)
        val difficultyRemaster: TextView = view.findViewById(R.id.levelRemaster)
        val songType: ImageView = view.findViewById(R.id.songType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_song, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return songList.size
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
        bgInnerStroke.setStroke(WindowsUtils.dp2px(holder.itemView.context,3f).toInt(),ContextCompat.getColor(holder.itemView.context,
            songData.getBgColor()))

        bgStroke.setStroke(
            WindowsUtils.dp2px(holder.itemView.context,4f).toInt(),
            ContextCompat.getColor(holder.itemView.context, songData.getStrokeColor())
        )

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
            selectLevel = getLevel(level)
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

    private fun getLevel(levelText: String): String {
        return when (levelText) {
            "LEVEL 1" -> return "1"
            "LEVEL 2" -> return "2"
            "LEVEL 3" -> return "3"
            "LEVEL 4" -> return "4"
            "LEVEL 5" -> return "5"
            "LEVEL 6" -> return "6"
            "LEVEL 7" -> return "7"
            "LEVEL 7+" -> return "8+"
            "LEVEL 8" -> return "8"
            "LEVEL 8+" -> return "8+"
            "LEVEL 9" -> return "9"
            "LEVEL 9+" -> return "9+"
            "LEVEL 10" -> return "10"
            "LEVEL 10+" -> return "10+"
            "LEVEL 11" -> return "11"
            "LEVEL 11+" -> return "11+"
            "LEVEL 12" -> return "12"
            "LEVEL 12+" -> return "12+"
            "LEVEL 13" -> return "13"
            "LEVEL 13+" -> return "13+"
            "LEVEL 14" -> return "14"
            "LEVEL 14+" -> return "14+"
            "LEVEL 15" -> return "15"
            else -> "0"
        }
    }
}