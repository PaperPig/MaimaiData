package com.paperpig.maimaidata.repository

import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.versionCheck
import com.paperpig.maimaidata.widgets.Settings

object SongDataManager {

    private val spUtils = SharePreferencesUtils(MaimaiDataApplication.instance, "songInfo")

    private val remasterComparator = Comparator<SongData> { a, b ->
        when {
            a.ds.size < 5 && b.ds.size < 5 -> 0
            a.ds.size < 5 -> 1
            b.ds.size < 5 -> -1
            else -> 0
        }
    }

    private val remasterAscComparator = remasterComparator.thenBy { it.ds.getOrNull(4) }
    private val remasterDescComparator = remasterComparator.thenByDescending { it.ds.getOrNull(4) }


    var list = emptyList<SongData>()

    private var maxTotal = 0
    private var maxTap = 0
    private var maxHold = 0
    private var maxSlide = 0
    private var maxTouch = 0
    private var maxBreak = 0


    suspend fun loadData() {
        list = SongDataRepository().getData(MaimaiDataApplication.instance)
            .sortedByDescending { it.id.toInt() }
        // 获取所有 charts 的 notes 列表
        maxTotal = list.flatMap { it.charts.map { chart -> chart.notes } }
            .maxOfOrNull { notes -> notes.sum() } ?: 0

        // 遍历每个 SongData
        for (songData in list) {
            if (songData.basic_info.genre == Constants.GENRE_UTAGE)
                continue
            // 遍历每个 Chart
            for (chart in songData.charts) {
                val notes = chart.notes

                // 计算 maxTap（第一个值）
                if (notes.isNotEmpty()) {
                    maxTap = maxOf(maxTap, notes[0])
                }

                // 计算 maxHold（第二个值）
                if (notes.size > 1) {
                    maxHold = maxOf(maxHold, notes[1])
                }

                // 计算 maxSlide（第三个值）
                if (notes.size > 2) {
                    maxSlide = maxOf(maxSlide, notes[2])
                }

                // 计算 maxTouch（当 type 为 "DX" 时的第四个值）
                if (songData.type == Constants.CHART_TYPE_DX && notes.size > 3) {
                    maxTouch = maxOf(maxTouch, notes[3])
                }

                // 计算 maxBreak
                if (songData.type == Constants.CHART_TYPE_DX && notes.size > 4) {
                    // DX 类型的第五个值
                    maxBreak = maxOf(maxBreak, notes[4])
                } else if (songData.type == Constants.CHART_TYPE_SD && notes.size > 3) {
                    // SD 类型的第四个值
                    maxBreak = maxOf(maxBreak, notes[3])
                }
            }
        }
    }

    fun getMaxNotesList(): List<Int> {
        return listOf(
            maxTotal,
            maxTap,
            maxHold,
            maxSlide,
            maxTouch,
            maxBreak
        )
    }


    fun search(
        searchText: String = "",
        genreSortList: List<String> = emptyList(),
        versionSortList: List<String> = emptyList(),
        selectLevel: String? = null,
        levelDs: Double? = null,
        sequencing: String? = null,
        isShowFavor: Boolean = false
    ): List<SongData> {
        return list.filter { song ->
            // 歌曲名匹配
            val matchesSearch = when {
                searchText.isEmpty() || song.title.isEmpty() -> true
                else -> song.title.contains(searchText, true)
            }
            // 别称匹配
            val matchesAlias = when {
                !Settings.getEnableAliasSearch() -> false
                searchText.isEmpty() -> true
                song.alias == null -> false
                else -> song.alias!!.any { it.contains(searchText, true) }
            }

            // 流派匹配，默认不显示宴会场
            val matchesGenre = when {
                genreSortList.isNotEmpty() -> song.basic_info.genre in genreSortList
                else -> song.basic_info.genre != Constants.GENRE_UTAGE
            }

            // 版本匹配
            val matchesVersion = when {
                versionSortList.isNotEmpty() -> versionSortList.versionCheck(song.basic_info.from)
                else -> true
            }

            // 等级匹配
            val matchesLevel = selectLevel?.let { level ->
                when {
                    level == "ALL" -> true
                    // 根据sequencing确定检查的等级位置
                    sequencing?.startsWith("EXPERT") == true -> song.level.getOrNull(2) == level
                    sequencing?.startsWith("MASTER") == true -> song.level.getOrNull(3) == level
                    sequencing?.startsWith("RE:MASTER") == true -> song.level.getOrNull(4) == level
                    else -> song.level.contains(level)
                }
            } ?: true

            // 定数匹配
            val matchesDs = levelDs?.let { ds -> song.ds.contains(ds) } ?: true

            // 是否收藏
            val matchesFavorite = !isShowFavor || spUtils.isFavorite(song.id)

            (matchesSearch || matchesAlias) && matchesGenre && matchesVersion && matchesLevel && matchesDs && matchesFavorite
        }.let { filteredList ->
            when (sequencing) {
                "EXPERT-升序" -> filteredList.sortedBy { it.ds.getOrNull(2) }
                "EXPERT-降序" -> filteredList.sortedByDescending { it.ds.getOrNull(2) }
                "MASTER-升序" -> filteredList.sortedBy { it.ds.getOrNull(3) }
                "MASTER-降序" -> filteredList.sortedByDescending { it.ds.getOrNull(3) }
                "RE:MASTER-升序" -> filteredList.sortedWith(remasterAscComparator)
                "RE:MASTER-降序" -> filteredList.sortedWith(remasterDescComparator)
                else -> filteredList.toList()
            }
        }

    }
}