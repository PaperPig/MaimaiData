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
    val id: String,
    val level: List<String>,
    val title: String,
    val type: String
) : Parcelable {
    @ColorRes
    fun getBgColor() =
        when (basic_info.genre) {
            "POPSアニメ" -> R.color.pop
            "niconicoボーカロイド" -> R.color.vocal
            "東方Project" -> R.color.touhou
            "バラエティ" -> R.color.variety
            "ゲームバラエティ" -> R.color.variety
            "maimai" -> R.color.maimai
            else -> R.color.gekichuni
        }

    fun getStrokeColor() =
        when (basic_info.genre) {
            "POPSアニメ" -> R.color.pop_stroke
            "niconicoボーカロイド" -> R.color.vocal_stroke
            "東方Project" -> R.color.touhou_stroke
            "バラエティ" -> R.color.variety_stroke
            "ゲームバラエティ" -> R.color.variety_stroke
            "maimai" -> R.color.maimai_stroke
            else -> R.color.gekichuni_stroke
        }

    inner class BasicInfo(
        val artist: String,
        val bpm: Int,
        var from: String,
        val genre: String,
        val is_new: Boolean,
        val title: String,
        var image_url: String,
        var version: String
    ) : Serializable

    inner class Chart(
        val charter: String,
        val notes: List<Int>
    ) : Serializable

}

