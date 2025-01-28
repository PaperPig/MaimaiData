package com.paperpig.maimaidata.repository

import android.text.TextUtils
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.versionCheck
import java.util.Locale

object SongDataManager {
    private val spUtils = SharePreferencesUtils(MaimaiDataApplication.instance, "songInfo")

    private val remasterAscComparator: java.util.Comparator<SongData> = Comparator { a1, a2 ->

        if (a1.ds.size < 5 && a2.ds.size < 5) {
            0
        } else if (a1.ds.size < 5)
            1
        else if (a2.ds.size < 5) {
            -1
        } else a1.ds[4].compareTo(a2.ds[4])
    }

    private val remasterDescComparator: java.util.Comparator<SongData> = Comparator { a1, a2 ->

        if (a1.ds.size < 5 && a2.ds.size < 5) {
            0
        } else if (a1.ds.size < 5)
            1
        else if (a2.ds.size < 5) {
            -1
        } else a2.ds[4].compareTo(a1.ds[4])
    }


    var list = emptyList<SongData>()


    suspend fun loadData() {
        list = SongDataRepository().getData(MaimaiDataApplication.instance).sortedByDescending { it.id.toInt() }
    }

    fun search(
        searchText: String,
        genreSortList: List<String>,
        versionSortList: List<String>,
        selectLevel: String,
        sequencing: String,
        isShowFavor: Boolean
    ): List<SongData> {
        var searchList = search(searchText, genreSortList, versionSortList, isShowFavor)
        var isLevelBySort = true
        if (selectLevel == "ALL") isLevelBySort = false
        if (isLevelBySort) {
            searchList = searchList.filter {
                it.level.contains(selectLevel)
            }
        }
        if (sequencing != "默认排序") {
            searchList = when (sequencing) {
                "EXPERT-升序" ->
                    searchList.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                        .sortedBy { it.ds[2] }

                "EXPERT-降序" -> searchList.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[2] == selectLevel else true }
                    .sortedByDescending { it.ds[2] }

                "MASTER-升序" -> searchList.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                    .sortedBy { it.ds[3] }

                "MASTER-降序" -> searchList.filter { it.basic_info.genre != "宴会場" && if (isLevelBySort) return@filter it.level[3] == selectLevel else true }
                    .sortedByDescending { it.ds[3] }

                "RE:MASTER-升序" -> searchList.sortedWith(remasterAscComparator)
                    .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }

                "RE:MASTER-降序" -> searchList.sortedWith(remasterDescComparator)
                    .filter { if (isLevelBySort && it.level.size == 5) return@filter it.level[4] == selectLevel else true }

                else -> searchList
            }
        }
        return searchList

    }

    fun search(
        searchText: String,
        genreSortList: List<String>,
        versionSortList: List<String>,
        levelDs: Double,
        isShowFavor: Boolean
    ): List<SongData> {
        var searchList = search(searchText, genreSortList, versionSortList, isShowFavor)
        searchList = searchList.filter {
            it.ds.contains(levelDs)
        }
        return searchList
    }

    private fun search(
        searchText: String,
        genreSortList: List<String>,
        versionSortList: List<String>,
        isShowFavor: Boolean
    ): List<SongData> {
        val searchList = list.filter {
            if (searchText.isEmpty() || TextUtils.isEmpty(it.title)) {
                return@filter true
            }
            return@filter it.title.lowercase(Locale.ROOT)
                .contains(searchText.lowercase(Locale.ROOT))
        }.filter {
            if (genreSortList.isNotEmpty()) {
                return@filter genreSortList.contains(it.basic_info.genre)
            } else
                return@filter true
        }.filter {
            if (versionSortList.isNotEmpty()) {
                return@filter versionSortList.versionCheck(it.basic_info.from)
            } else return@filter true
        }.filter {
            if (isShowFavor) {
                spUtils.isFavorite(it.id)
            } else {
                true
            }
        }
        return searchList
    }


}