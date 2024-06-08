package com.paperpig.maimaidata.model

data class Rating(
    /**
     * 定数
     */
    val innerLevel: Float,

    /**
     * 达成率
     */
    val achi: String,

    /**
     * 单曲rating
     */
    val rating: Int,

    /**
     * 合计rating
     */
    val total: Int
)
