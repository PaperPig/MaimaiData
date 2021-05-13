package com.paperpig.maimaidata.model

import androidx.annotation.ColorRes
import com.paperpig.maimaidata.R
import java.io.Serializable


data class SongData(
    val basic_info: BasicInfo,
    val charts: List<Chart>,
    val ds: List<Double>,
    val id: String,
    val level: List<String>,
    val title: String,
    val type: String
) : Serializable {
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


}

data class BasicInfo(
    val artist: String,
    val bpm: Int,
    val from: String,
    val genre: String,
    val is_new: Boolean,
    var release_date: String,
    val title: String,
    var title_kana: String,
    var image_url: String
):Serializable

data class Chart(
    val charter: String,
    val notes: List<Int>
):Serializable


fun totalScore(note: List<Int>, isDx: Boolean): Int {
    return if (isDx) {
        (note[0] + note[3]) + note[1] * 2 + note[2] * 3 + note[4] * 5
    } else {
        note[0] + note[1] * 2 + note[2] * 3 + note[3] * 5
    }
}
