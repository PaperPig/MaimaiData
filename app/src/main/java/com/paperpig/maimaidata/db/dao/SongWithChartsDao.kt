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
import com.paperpig.maimaidata.model.DifficultyType
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


    //todo 别称匹配查询
    /**
     *  根据指定的条件搜索歌曲数据及其关联的谱面信息。
     *  该查询允许根据歌曲标题、流派、版本、难度等级、谱面类型（通过 sequencing 参数）和定数进行筛选。
     */
    @Query(
        """
        SELECT * FROM song_data 
        WHERE 
            -- 标题匹配
            title LIKE '%' || :searchText || '%'
        
            -- 流派匹配
            AND (
                (:isGenreListEmpty = 1 AND genre != '${Constants.GENRE_UTAGE}')
                OR (:isGenreListEmpty = 0 AND genre IN (:genreList))
            )
        
            -- 版本匹配
            AND (
                :isVersionListEmpty = 1 
                OR `from` IN (:versionList)
            )
        
            -- 等级匹配
            AND (
                :selectLevel IS NULL 
                OR id IN (
                    SELECT DISTINCT song_id
                    FROM chart
                    WHERE (:selectLevel = 'ALL' 
                    -- 指定具体等级时匹配指定排序的难度
                        OR (level = :selectLevel AND (:sequencing IS NULL OR difficulty_type = :sequencing))) 
                )
            )

            -- 定数匹配
            AND (
                :ds IS NULL 
                OR id IN (
                    SELECT DISTINCT song_id
                    FROM chart
                    WHERE ds = :ds
                )
            )
            
            -- 收藏匹配
            AND (
                :isSearchFavor = 1 AND id IN (:favIdList) 
                OR :isSearchFavor = 0
                )
        """
    )
    fun searchSongsWithCharts(
        searchText: String,
        isGenreListEmpty: Boolean,
        genreList: List<String>,
        isVersionListEmpty: Boolean,
        versionList: List<String>,
        sequencing: DifficultyType?,
        selectLevel: String?,
        ds: Double?,
        isSearchFavor: Boolean,
        favIdList: List<String>
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