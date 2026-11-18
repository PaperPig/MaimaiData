package com.paperpig.maimaidata.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.dao.PlayerProfileDao
import com.paperpig.maimaidata.db.entity.PlayerProfileEntity
import com.paperpig.maimaidata.model.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App 本地玩家个人信息仓库（持久化到 Room 数据库）。
 */
class PlayerProfileRepository private constructor(
    private val playerProfileDao: PlayerProfileDao
) {
    companion object {
        @Volatile
        private var instance: PlayerProfileRepository? = null

        fun getInstance(): PlayerProfileRepository {
            return getInstance(AppDataBase.getInstance().playerProfileDao())
        }

        fun getInstance(playerProfileDao: PlayerProfileDao): PlayerProfileRepository {
            return instance ?: synchronized(this) {
                instance ?: PlayerProfileRepository(playerProfileDao).also {
                    instance = it
                }
            }
        }
    }

    fun getProfileLiveData(): LiveData<PlayerProfile?> {
        return playerProfileDao.getProfileLiveData().map { it?.toModel() }
    }

    suspend fun getProfileOnce(): PlayerProfile? = withContext(Dispatchers.IO) {
        playerProfileDao.getProfileOnce()?.toModel()
    }

    suspend fun saveProfile(profile: PlayerProfile) = withContext(Dispatchers.IO) {
        playerProfileDao.upsertProfile(PlayerProfileEntity.fromModel(profile))
    }

    suspend fun clearProfile() = withContext(Dispatchers.IO) {
        playerProfileDao.clearProfile()
    }
}
