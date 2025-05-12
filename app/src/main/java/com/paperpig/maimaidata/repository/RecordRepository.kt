package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import com.paperpig.maimaidata.db.dao.RecordDao
import com.paperpig.maimaidata.db.entity.RecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecordRepository private constructor(private val recordDao: RecordDao) {
    companion object {
        @Volatile
        private var instance: RecordRepository? = null

        fun getInstance(recordDao: RecordDao): RecordRepository {
            if (instance == null) {
                instance = RecordRepository(recordDao)
            }
            return instance!!
        }
    }


    /**
     * 更新本地成绩数据库
     */
    suspend fun replaceAllRecord(list: List<RecordEntity>): Boolean {
        return withContext(Dispatchers.IO) {
            recordDao.replaceAllRecord(list)
        }
    }

    /**
     * 获取所有成绩
     */
    fun getAllRecord(): LiveData<List<RecordEntity>> {
        return recordDao.getAllRecords()
    }

    /**
     * 根据难度索引获取成绩
     * @param index 0 = basic , 1 = advance , 2 = expert , 3 = master , 4 = remaster
     * @return 成绩列表
     */
    fun getRecordsByDifficultyIndex(index: Int): LiveData<List<RecordEntity>> {
        return recordDao.getRecordsByDifficultyIndex(index)
    }


    /**
     * 根据歌曲ID获取成绩
     * @param songId 歌曲ID
     * @return 成绩列表
     */
    fun getRecordsBySongId(songId: Int): LiveData<List<RecordEntity>> {
        return recordDao.getRecordsBySongId(songId)
    }

}