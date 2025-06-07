package com.paperpig.maimaidata.model

import android.os.Parcelable
import androidx.annotation.ColorRes
import com.paperpig.maimaidata.R
import kotlinx.parcelize.Parcelize
import java.io.Serializable


@Parcelize
data class SongData(
    val basic_info: BasicInfo,
    val charts: List<Chart>,
    val ds: List<Double>,
    var old_ds: List<Double>,
    val id: String,
    var level: List<String>,
    val title: String,
    var title_kana: String,
    val type: String,
    var alias: List<String>?,
) : Parcelable {
    @ColorRes
    fun getBgColor() =
        when (basic_info.genre) {
            "流行&动漫" -> R.color.pop
            "niconico & VOCALOID" -> R.color.vocal
            "东方Project" -> R.color.touhou
            "其他游戏" -> R.color.variety
            "舞萌" -> R.color.maimai
            "宴会場" -> R.color.utage
            else -> R.color.gekichuni
        }

    fun getStrokeColor() =
        when (basic_info.genre) {
            "流行&动漫" -> R.color.pop_stroke
            "niconico & VOCALOID" -> R.color.vocal_stroke
            "东方Project" -> R.color.touhou_stroke
            "其他游戏" -> R.color.variety_stroke
            "舞萌" -> R.color.maimai_stroke
            "宴会場" -> R.color.utage_stroke
            else -> R.color.gekichuni_stroke
        }


    inner class BasicInfo(
        val artist: String,
        val bpm: Int,
        var from: String,
        var genre: String,
        var catcode: String,
        val is_new: Boolean,
        val title: String,
        var image_url: String,
        var version: String,
        var kanji: String?,
        var comment: String?,
        var buddy: String?
    ) : Serializable

    inner class Chart(
        val charter: String,
        val notes: List<Int>
    ) : Serializable

}

