package com.paperpig.maimaidata.repository

import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.model.ChartStatus

object ChartStatsManager {
    var list: Map<String, List<ChartStatus>> = mapOf()

    suspend fun loadData() {
        list = ChartStatsRepository().getChartStats(MaimaiDataApplication.instance)
    }
}