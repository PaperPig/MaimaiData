package com.paperpig.maimaidata.model

import com.paperpig.maimaidata.R

data class Record(
    val achievements: Double,
    val ds: Double,
    val dxScore: Int,
    val fc: String,
    val fs: String,
    val is_new: Boolean,
    val level: String,
    val level_index: Int,
    val level_label: String,
    val ra: Int,
    val rate: String,
    val song_id: String,
    val title: String,
    val type: String

) {
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

    fun getDifficultyDiff() = when (level_index) {
        0 -> R.drawable.mmd_player_rtsong_diff_bsc
        1 -> R.drawable.mmd_player_rtsong_diff_adv
        2 -> R.drawable.mmd_player_rtsong_diff_exp
        3 -> R.drawable.mmd_player_rtsong_diff_mst
        else -> R.drawable.mmd_player_rtsong_diff_rem
    }

    fun getBackgroundColor() = when (level_index) {
        0 -> R.color.mmd_player_rtsong_bsc_main
        1 -> R.color.mmd_player_rtsong_adv_main
        2 -> R.color.mmd_player_rtsong_exp_main
        3 -> R.color.mmd_player_rtsong_mst_main
        else -> R.color.mmd_player_rtsong_rem_main
    }


    fun getShadowColor() = when (level_index) {
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
        return if (type == "DX")
            R.drawable.mmd_player_rtsong_icon_dx
        else R.drawable.mmd_player_rtsong_icon_standard
    }

    fun getRatingBoard(): Int = when (level_index) {
        0 -> R.drawable.mmd_rating_board_bsc
        1 -> R.drawable.mmd_rating_board_adv
        2 -> R.drawable.mmd_rating_board_exp
        3 -> R.drawable.mmd_rating_board_mas
        else -> R.drawable.mmd_rating_board_rem
    }

    fun getRatingDiff() = when (level_index) {
        0 -> R.drawable.mmd_rating_diff_basic
        1 -> R.drawable.mmd_rating_diff_advanced
        2 -> R.drawable.mmd_rating_diff_expert
        3 -> R.drawable.mmd_rating_diff_master
        else -> R.drawable.mmd_rating_diff_remaster
    }
}