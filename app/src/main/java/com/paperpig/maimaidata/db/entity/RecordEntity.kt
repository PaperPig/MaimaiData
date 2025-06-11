package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.utils.Constants
import kotlinx.parcelize.Parcelize

@Entity(tableName = "record")
@Parcelize
data class RecordEntity(
    // 主键（自增长，默认值 0）
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 完成率
    @ColumnInfo(name = "achievements")
    val achievements: Double,

    // 定数
    @ColumnInfo(name = "ds")
    val ds: Double,

    // DX分数
    @ColumnInfo(name = "dx_score")
    val dxScore: Int,

    // 全连状态
    @ColumnInfo(name = "fc")
    val fc: String,

    // 同步状态
    @ColumnInfo(name = "fs")
    val fs: String,

    // 等级
    @ColumnInfo(name = "level")
    val level: String,

    // 等级索引
    @SerializedName("level_index")
    @ColumnInfo(name = "level_index")
    val levelIndex: Int,

    // 等级标签
    @SerializedName("level_label")
    @ColumnInfo(name = "level_label")
    val levelLabel: String,

    // Rating值
    @ColumnInfo(name = "ra")
    val ra: Int,

    // 评级
    @ColumnInfo(name = "rate")
    val rate: String,

    // 歌曲ID
    @SerializedName("song_id")
    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    // 歌曲标题
    @ColumnInfo(name = "title")
    val title: String,

    // 标准 or DX
    @ColumnInfo(name = "type")
    val type: String
) : Parcelable {
    fun getFcIcon() = when (fc) {
        "fc" -> R.drawable.mmd_player_rtsong_fc
        "fcp" -> R.drawable.mmd_player_rtsong_fcp
        "ap" -> R.drawable.mmd_player_rtsong_ap
        "app" -> R.drawable.mmd_player_rtsong_app
        else -> R.drawable.mmd_player_rtsong_stub
    }

    fun getFsIcon() = when (fs) {
        "fs" -> R.drawable.mmd_player_rtsong_fs
        "fsp" -> R.drawable.mmd_player_rtsong_fsp
        "fsd" -> R.drawable.mmd_player_rtsong_fsd
        "fsdp" -> R.drawable.mmd_player_rtsong_fsdp
        else -> R.drawable.mmd_player_rtsong_stub
    }

    fun getDifficultyDiff() = when (levelIndex) {
        0 -> R.drawable.mmd_player_rtsong_diff_bsc
        1 -> R.drawable.mmd_player_rtsong_diff_adv
        2 -> R.drawable.mmd_player_rtsong_diff_exp
        3 -> R.drawable.mmd_player_rtsong_diff_mst
        else -> R.drawable.mmd_player_rtsong_diff_rem
    }

    fun getBackgroundColor() = when (levelIndex) {
        0 -> R.color.mmd_player_rtsong_bsc_main
        1 -> R.color.mmd_player_rtsong_adv_main
        2 -> R.color.mmd_player_rtsong_exp_main
        3 -> R.color.mmd_player_rtsong_mst_main
        else -> R.color.mmd_player_rtsong_rem_main
    }


    fun getShadowColor() = when (levelIndex) {
        0 -> R.color.mmd_player_rtsong_bsc_dark
        1 -> R.color.mmd_player_rtsong_adv_dark
        2 -> R.color.mmd_player_rtsong_exp_dark
        3 -> R.color.mmd_player_rtsong_mst_dark
        else -> R.color.mmd_player_rtsong_rem_dark
    }

    fun getRankIcon() = when (rate) {
        "d" -> R.drawable.mmd_player_rtsong_d
        "c" -> R.drawable.mmd_player_rtsong_c
        "b" -> R.drawable.mmd_player_rtsong_b
        "bb" -> R.drawable.mmd_player_rtsong_bb
        "bbb" -> R.drawable.mmd_player_rtsong_bbb
        "a" -> R.drawable.mmd_player_rtsong_a
        "aa" -> R.drawable.mmd_player_rtsong_aa
        "aaa" -> R.drawable.mmd_player_rtsong_aaa
        "s" -> R.drawable.mmd_player_rtsong_s
        "sp" -> R.drawable.mmd_player_rtsong_sp
        "ss" -> R.drawable.mmd_player_rtsong_ss
        "ssp" -> R.drawable.mmd_player_rtsong_ssp
        "sss" -> R.drawable.mmd_player_rtsong_sss
        "sssp" -> R.drawable.mmd_player_rtsong_sssp
        else -> R.drawable.mmd_player_rtsong_d
    }


    fun getTypeIcon(): Int {
        return if (type == Constants.CHART_TYPE_DX)
            R.drawable.mmd_player_rtsong_icon_dx
        else R.drawable.mmd_player_rtsong_icon_standard
    }

    fun getRatingBoard(): Int = when (levelIndex) {
        0 -> R.drawable.mmd_rating_board_bsc
        1 -> R.drawable.mmd_rating_board_adv
        2 -> R.drawable.mmd_rating_board_exp
        3 -> R.drawable.mmd_rating_board_mas
        else -> R.drawable.mmd_rating_board_rem
    }

    fun getRatingDiff() = when (levelIndex) {
        0 -> R.drawable.mmd_rating_diff_basic
        1 -> R.drawable.mmd_rating_diff_advanced
        2 -> R.drawable.mmd_rating_diff_expert
        3 -> R.drawable.mmd_rating_diff_master
        else -> R.drawable.mmd_rating_diff_remaster
    }
}

