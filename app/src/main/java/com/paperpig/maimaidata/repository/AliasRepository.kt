package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.dao.AliasDao
import com.paperpig.maimaidata.db.entity.AliasEntity

class AliasRepository private constructor(private val aliasDao: AliasDao) {
    companion object {
        @Volatile
        private var instance: AliasRepository? = null
        fun getInstance(songChartDao: AliasDao): AliasRepository {
            if (instance == null) {
                instance = AliasRepository(songChartDao)
            }
            return instance!!
        }
    }

    /**
     * 通过歌曲id搜索别名列表
     */
    fun getAliasListBySongId(id: Int): LiveData<List<AliasEntity>> {
        return aliasDao.getAliasListBySongId(id)
    }
}

