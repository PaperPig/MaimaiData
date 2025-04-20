package com.paperpig.maimaidata.model

/**
 * 用于定数排序的歌曲
 */
data class DsSongData(
    /**
     * 歌曲id
     */
    val songId: Int,

    /**
     * 歌曲标题
     */
    val title: String,

    /**
     * 谱面类型
     */
    val type: String,

    /**
     * 曲封url
     */
    val imageUrl: String?,

    /**
     * 难度索引
     */
    val levelIndex: Int,

    /**
     * 定数
     */
    val ds: Double
)
