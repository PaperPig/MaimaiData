package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.dao.ChartStatsDao
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChartStatsRepository private constructor(val chartStatsDao: ChartStatsDao) {
    companion object {
        @Volatile
        private var instance: ChartStatsRepository? = null

        fun getInstance(chartStatsDao: ChartStatsDao): ChartStatsRepository {
            return instance ?: synchronized(this) {
                instance ?: ChartStatsRepository(chartStatsDao).also { instance = it }
            }
        }
    }

    suspend fun replaceAllChartStats(list: List<ChartStatsEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            chartStatsDao.replaceAllChartStats(list)
        }
    }

    fun getChartStatsBySongIdAndDifficultyIndex(
        songId: Int,
        index: Int
    ): LiveData<ChartStatsEntity> {
        return chartStatsDao.getChartStatsBySongIdAndDifficultyIndex(songId, index)
    }
}