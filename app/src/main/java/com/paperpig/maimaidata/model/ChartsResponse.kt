package com.paperpig.maimaidata.model

import com.google.gson.annotations.SerializedName


data class ChartsResponse(
    @SerializedName("charts")
    val charts: Map<String, List<ChartData>>
)

data class ChartData(
    @SerializedName("cnt")
    val cnt: Double?,
    @SerializedName("diff")
    val diff: String?,
    @SerializedName("fit_diff")
    val fitDiff: Double?,
    @SerializedName("avg")
    val avg: Double?,
    @SerializedName("avg_dx")
    val avgDx: Double?,
    @SerializedName("std_dev")
    val stdDev: Double?,
    @SerializedName("dist")
    val dist: List<Int>?,
    @SerializedName("fc_dist")
    val fcDist: List<Int>?
)