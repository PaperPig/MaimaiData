package com.paperpig.maimaidata.db.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.utils.Constants

@Dao
interface SongWithChartsDao : ChartDao, SongDao, AliasDao {

    /**
     * 批量替换所有歌曲、谱面和别名数据。
     * @param songDataList 歌曲列表
     * @param chartList 谱面列表
     * @param aliasList 别名列表
     * @return 返回操作结果。
     */
    @Transaction
    fun replaceAllSongsAndCharts(
        songDataList: List<SongDataEntity>,
        chartList: List<ChartEntity>,
        aliasList: List<AliasEntity>
    ): Boolean {
        return try {
            clearSongData()
            clearCharts()
            clearAlias()

            insertAllSongs(songDataList)
            insertAllCharts(chartList)
            insertAllAlias(aliasList)
            true
        } catch (e: Exception) {
            Log.e(AppDataBase.DATABASE_NAME, "Transaction failed: ${e.message}")
            false
        }
    }


    /**
     * 获取所有歌曲及其关联的谱面信息。
     * @param includeUtage 是否包含宴曲目（`genre != UTAGE`）。
     * @param ascending 是否按升序排列（`id ASC`）。
     * @return 包含歌曲及其关联谱面的实体列表（通过 LiveData 返回，便于 UI 响应式刷新）。
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


    /**
     * 根据指定的筛选条件搜索歌曲数据，并获取其关联的谱面信息。
     *
     * 支持的筛选条件包括：
     * - 歌曲标题或别名（模糊匹配）
     * - 歌曲流派
     * - 歌曲所属版本
     * - 谱面等级（可指定难度类型）
     * - 谱面定数（ds）
     * - 是否为收藏歌曲
     *
     * @param searchText 搜索关键字，会用于模糊匹配主标题和（可选）别名。
     * @param isGenreListEmpty 是否启用流派筛选。
     *        - 若为 true，则排除“宴”曲目（`genre != UTAGE`）。
     *        - 若为 false，则只保留在 genreList 中的流派。
     * @param genreList 流派列表，用于筛选（当 isGenreListEmpty 为 false 时生效）。
     * @param isVersionListEmpty 是否启用版本筛选。
     *        - 若为 true，不限制版本。
     *        - 若为 false，仅包含在 versionList 中的版本。
     * @param versionList 版本列表，用于筛选（当 isVersionListEmpty 为 false 时生效）。
     * @param sequencing 谱面难度类型，如 BASIC、ADVANCED、EXPERT、MASTER、REMASTER 等。为 null 表示不筛选。
     * @param selectLevel 筛选的谱面等级（如 "8+", "9" 等），为 null 表示不筛选。
     *        - 如果为 "ALL"，则匹配任意等级。
     *        - 否则仅匹配指定等级，并可进一步通过 sequencing 限定难度类型。
     * @param ds 筛选的定数（难度值），为 null 表示不筛选。
     * @param isSearchFavor 是否只搜索收藏的歌曲。
     *        - 若为 true，则只包含在 favIdList 中的歌曲。
     *        - 若为 false，则不限制。
     * @param favIdList 收藏歌曲的 ID 列表（只有当 isSearchFavor 为 true 时生效）。
     * @param isMatchAlias 是否启用别名匹配。
     *        - 若为 true，除了主标题外也会匹配 alias 表中的别名。
     *        - 若为 false，只匹配主标题。
     *
     * @return 包含歌曲及其关联谱面的实体列表（通过 LiveData 返回，便于 UI 响应式刷新）。
     */
    @Query(
        """
        SELECT * FROM song_data 
        WHERE 
            -- 标题匹配（主标题或别名）
            (
                title LIKE '%' || :searchText || '%'
                OR (
                    :isMatchAlias = 1 
                    AND EXISTS(
                        SELECT 1 
                        FROM alias a 
                        WHERE 
                            a.song_id = song_data.id 
                            AND a.alias LIKE '%' || :searchText || '%'
                    )
                )
            )
            
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
                    WHERE 
                        :selectLevel = 'ALL' 
                        OR (
                            level = :selectLevel 
                            AND (:sequencing IS NULL OR difficulty_type = :sequencing)
                        ) 
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
            
            -- 收藏歌曲匹配
            AND (
                (:isSearchFavor = 1 AND id IN (:favIdList))
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
        favIdList: List<String>,
        isMatchAlias: Boolean
    ): LiveData<List<SongWithChartsEntity>>


}