package com.paperpig.maimaidata.db.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.utils.Constants

@Dao
interface SongWithChartsDao {


    /**
     * 事务函数，用于替换所有歌曲和谱面数据
     */
    @Transaction
    fun replaceAllSongsAndCharts(
        songDataList: List<SongDataEntity>,
        chartList: List<ChartEntity>
    ): Boolean {
        return try {
            clearSongData()
            clearCharts()
            insertAllSongs(songDataList)
            insertAllCharts(chartList)
            true
        } catch (e: Exception) {
            Log.e(AppDataBase.DATABASE_NAME, "Transaction failed: ${e.message}")
            false
        }
    }

    /**
     *  获取所有歌曲和对应的谱面数据
     *  如果includeUtage为true，包含所有歌曲，包括类型为"UTAGE"的歌曲
     *  如果ascending为true，则按id升序排序；如果为false，则按id降序排序
     */
    @Query(
        """
        SELECT * FROM song_data
        WHERE (:includeUtage = 1 OR genre != '${Constants.GENRE_UTAGE}')
        ORDER BY 
            CASE WHEN :ascending = 1 THEN id END ASC,
            CASE WHEN :ascending = 0 THEN id END DESC
        """
    )
    fun getAllSongsWithCharts(
        includeUtage: Boolean = true,
        ascending: Boolean = false
    ): LiveData<List<SongWithChartsEntity>>


    @Insert
    fun insertAllCharts(chartList: List<ChartEntity>)

    @Insert
    fun insertAllSongs(songDataList: List<SongDataEntity>)

    @Query("DELETE FROM song_data")
    fun clearSongData()

    @Query("DELETE FROM chart")
    fun clearCharts()
}