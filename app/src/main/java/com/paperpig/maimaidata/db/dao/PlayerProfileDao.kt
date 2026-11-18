package com.paperpig.maimaidata.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.paperpig.maimaidata.db.entity.PlayerProfileEntity

@Dao
interface PlayerProfileDao {
    @Query("SELECT * FROM player_profile WHERE id = ${PlayerProfileEntity.SINGLE_PROFILE_ID} LIMIT 1")
    fun getProfileLiveData(): LiveData<PlayerProfileEntity?>

    @Query("SELECT * FROM player_profile WHERE id = ${PlayerProfileEntity.SINGLE_PROFILE_ID} LIMIT 1")
    fun getProfileOnce(): PlayerProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertProfile(entity: PlayerProfileEntity)

    @Query("DELETE FROM player_profile")
    fun clearProfile()
}
