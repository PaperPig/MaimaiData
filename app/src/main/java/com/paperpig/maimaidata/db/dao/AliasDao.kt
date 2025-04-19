package com.paperpig.maimaidata.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.AliasEntity

@Dao
interface AliasDao {

    /**
     * 根据歌曲 ID 查询对应的别名列表。
     *
     * @param songId 歌曲的唯一标识 ID。
     * @return 包含该歌曲所有别名的 LiveData 列表，用于响应式更新 UI。
     */
    @Query("SELECT * FROM alias WHERE song_id = :songId")
    fun getAliasListBySongId(songId: Int): LiveData<List<AliasEntity>>


    @Insert
    fun insertAllAlias(aliasList: List<AliasEntity>)


    @Query("DELETE  FROM alias")
    fun clearAlias()
}