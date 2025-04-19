package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.db.dao.SongWithChartsDao
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SharePreferencesUtils
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
                convertToChartEntities.songs,
                convertToChartEntities.charts,
                convertToChartEntities.aliases
            )
        }
    }

    /**
     * 获取所有歌曲和对应谱面信息
     */
    fun getAllSongWithCharts(
        includeUtage: Boolean = false,
        ascending: Boolean = false
    ): LiveData<List<SongWithChartsEntity>> {
        return songChartDao.getAllSongsWithCharts(includeUtage, ascending)
    }


    /**
     * 根据搜索文本、歌曲类型、版本、难度等级、流派和 定数 值搜索歌曲及其谱面信息。
     *
     * @param searchText 搜索文本，用于匹配歌曲名称。
     * @param genreList 歌曲流派列表，用于筛选特定流派的歌曲。如果为空，则不按流派筛选。
     * @param versionList 版本列表，用于筛选特定版本的歌曲。如果为空，则不按版本筛选。
     * @param selectLevel 可选的难度等级，用于筛选特定难度等级的谱面。可以为 null，表示不按难度等级筛选。
     * @param sequencing 可选的排序信息，用于排序指定难度的歌曲。会提取 "EXPERT" 或 "MASTER" 前缀进行筛选。可以为 null，表示默认排序。
     * @param ds 可选的定数值，用于筛选特定定数值的谱面。可以为 null，表示不按ds值筛选。
     * @param isFavor 是否搜索收藏的歌曲，设置为true时，使用sp文件中收藏歌曲的id进行查询
     * @param isMatchAlias 是否匹配别名搜索，用于匹配歌曲别名
     * @return 包含符合搜索条件的 SongWithChartsEntity 列表的 LiveData。
     */
    fun searchSongsWithCharts(
        searchText: String,
        genreList: List<String>,
        versionList: List<String>,
        selectLevel: String?,
        sequencing: String?,
        ds: Double?,
        isFavor: Boolean,
        isMatchAlias: Boolean,
    ): LiveData<List<SongWithChartsEntity>> {
        val initialResult = songChartDao.searchSongsWithCharts(
            searchText = searchText,
            isGenreListEmpty = genreList.isEmpty(),
            genreList = genreList,
            isVersionListEmpty = versionList.isEmpty(),
            versionList = expandFromList(versionList),
            selectLevel = selectLevel,
            sequencing = getDifficultyPrefix(sequencing),
            ds = ds,
            isSearchFavor = isFavor,
            favIdList = SharePreferencesUtils(
                MaimaiDataApplication.instance,
                SharePreferencesUtils.PREF_NAME_SONG_INFO
            ).getFavIds(),
            isMatchAlias = isMatchAlias
        )
        //根据sequencing指定难度排序
        return initialResult.map { list ->
            if (sequencing.isNullOrBlank()) {
                list.sortedByDescending { it.songData.id }
            } else {
                when (sequencing) {
                    "EXPERT-升序" -> list.sortedBy { it.charts.getOrNull(2)?.ds }
                    "EXPERT-降序" -> list.sortedByDescending { it.charts.getOrNull(2)?.ds }
                    "MASTER-升序" -> list.sortedBy { it.charts.getOrNull(3)?.ds }
                    "MASTER-降序" -> list.sortedByDescending { it.charts.getOrNull(3)?.ds }
                    "RE:MASTER-升序" -> list.sortedWith(remasterAscComparator)
                    "RE:MASTER-降序" -> list.sortedWith(remasterDescComparator)
                    else -> list.sortedByDescending { it.songData.id }
                }
            }
        }
    }


    private fun getDifficultyPrefix(sequencing: String?): DifficultyType? {
        if (sequencing == null) return null
        return when {
            sequencing.startsWith("EXPERT") -> DifficultyType.EXPERT
            sequencing.startsWith("MASTER") -> DifficultyType.MASTER
            sequencing.startsWith("RE:MASTER") -> DifficultyType.REMASTER
            else -> null
        }
    }


    private fun expandFromList(versionList: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (item in versionList) {

            if (item == "maimai") {
                result.add(item)
                result.add("$item PLUS")
            } else if (item == "FiNALE") {
                result.add("maimai $item")
            } else if (!item.startsWith("舞萌") && !item.contains("FiNALE")) {
                result.add("maimai $item PLUS")
                result.add("maimai $item")
            } else {
                result.add(item)
            }
        }
        return result
    }

    private val remasterComparator = Comparator<SongWithChartsEntity> { a, b ->
        when {
            a.charts.size < 5 && b.charts.size < 5 -> 0
            a.charts.size < 5 -> 1
            b.charts.size < 5 -> -1
            else -> 0
        }
    }

    private val remasterAscComparator = remasterComparator.thenBy { it.charts.getOrNull(4)?.ds }
    private val remasterDescComparator =
        remasterComparator.thenByDescending { it.charts.getOrNull(4)?.ds }

}

