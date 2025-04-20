package com.paperpig.maimaidata.db.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity

@Dao
interface RecordDao {

    /**
     * 批量替换所有记录数据。
     * @param recordList 记录列表
     * @return 操作结果
     */
    @Transaction
    fun replaceAllRecord(recordList: List<RecordEntity>): Boolean {
        try {
            clearRecord()
            insertAllRecord(recordList)
            return true
        } catch (e: Exception) {
            Log.e(AppDataBase.DATABASE_NAME, "Transaction replaceAllRecord failed: ${e.message}")
            return false
        }
    }

    @Query("SELECT * FROM record")
    fun getAllRecords(): LiveData<List<RecordEntity>>

    /**
     * 根据难度索引查询记录表
     * @param index 难度索引
     * @return 记录列表
     */
    @Query("SELECT * FROM record WHERE level_index = :index")
    fun getRecordsByDifficultyIndex(index: Int): LiveData<List<RecordEntity>>


    /**
     * 根据歌曲ID获取记录表
     * @param songId 歌曲ID
     * @return 记录列表
     */
    @Query("SELECT * FROM record WHERE song_id = :songId")
    fun getRecordsBySongId(songId: Int): LiveData<List<RecordEntity>>

    @Insert
    fun insertAllRecord(list: List<RecordEntity>)

    @Query("DELETE FROM record")
    fun clearRecord()
}