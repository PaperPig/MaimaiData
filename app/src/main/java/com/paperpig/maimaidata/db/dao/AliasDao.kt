package com.paperpig.maimaidata.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.AliasEntity

@Dao
interface AliasDao {

    @Query("SELECT * FROM alias WHERE song_id = :songId")
    fun getAliasListBySongId(songId: Int): LiveData<List<AliasEntity>>


    @Insert
    fun insertAllAlias(aliasList: List<AliasEntity>)


    @Query("DELETE  FROM alias")
    fun clearAlias()
}