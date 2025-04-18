package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.R
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "song_data")
data class SongDataEntity(
    // 主键（歌曲id）
    @PrimaryKey
    val id: Int,

    // 标题
    val title: String,

    // 标题假名（有汉字时）
    @ColumnInfo(name = "title_kana")
    val titleKana: String,

    // 作曲家
    val artist: String,

    // 图片地址
    @ColumnInfo(name = "image_url")
    val imageUrl: String,

    // 歌曲流派
    val genre: String,

    // 歌曲流派（日）
    @ColumnInfo(name = "cat_code")
    val catCode: String,

    // bpm
    val bpm: Int,

    // 添加版本
    val from: String,

    // 标准 or DX
    val type: String,

    // 添加版本（日）
    val version: String,

    // 是否为新版本歌曲
    @ColumnInfo(name = "is_new")
    val isNew: Boolean,

    // 宴会场分类汉字
    val kanji: String?,

    // 宴会场说明
    val comment: String?,

    // 双人协谱标记
    val buddy: String?,
) : Parcelable {
    @IgnoredOnParcel
    @Ignore
    @ColorRes
    val bgColor: Int = when (genre) {
        "流行&动漫" -> R.color.pop
        "niconico & VOCALOID" -> R.color.vocal
        "东方Project" -> R.color.touhou
        "其他游戏" -> R.color.variety
        "舞萌" -> R.color.maimai
        "宴会場" -> R.color.utage
        else -> R.color.gekichuni
    }

    @IgnoredOnParcel
    @Ignore
    @ColorRes
    val strokeColor: Int =
        when (genre) {
            "流行&动漫" -> R.color.pop_stroke
            "niconico & VOCALOID" -> R.color.vocal_stroke
            "东方Project" -> R.color.touhou_stroke
            "其他游戏" -> R.color.variety_stroke
            "舞萌" -> R.color.maimai_stroke
            "宴会場" -> R.color.utage_stroke
            else -> R.color.gekichuni_stroke
        }
}


