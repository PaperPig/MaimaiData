package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.dao.SongWithChartsDao
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.JsonConvertToDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongWithChartRepository private constructor(private val songChartDao: SongWithChartsDao) {
    companion object {
        @Volatile
        private var instance: SongWithChartRepository? = null
        fun getInstance(songChartDao: SongWithChartsDao): SongWithChartRepository {
            if (instance == null) {
                instance = SongWithChartRepository(songChartDao)
            }
            return instance!!
        }
    }

    /**
     * 更新本地歌曲谱面数据库信息
     */
    suspend fun updateDatabase(list: List<SongData>): Boolean {
        return withContext(Dispatchers.IO) {
            val convertToChartEntities = JsonConvertToDb.convert(list)
            songChartDao.replaceAllSongsAndCharts(
                convertToChartEntities.first,
                convertToChartEntities.second
            )
        }
    }

    /**
     * 获取所有歌曲和对应谱面信息
     */
    fun getAllSongAndCharts(
        includeUtage: Boolean = true,
        ascending: Boolean = false
    ): LiveData<List<SongWithChartsEntity>> {
        return songChartDao.getAllSongsWithCharts(includeUtage, ascending)
    }


}

