package com.paperpig.maimaidata.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.db.AppDataBase.Companion.DATABASE_VERSION
import com.paperpig.maimaidata.db.dao.ChartDao
import com.paperpig.maimaidata.db.dao.SongDao
import com.paperpig.maimaidata.db.dao.SongWithChartsDao
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity

@Database(
    entities = [SongDataEntity::class, ChartEntity::class],
    version = DATABASE_VERSION
)
abstract class AppDataBase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun chartDao(): ChartDao
    abstract fun songWithChartDao(): SongWithChartsDao


    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "maimaidata_db"

        @Volatile
        private lateinit var instance: AppDataBase


        fun getInstance(): AppDataBase {
            if (!::instance.isInitialized) {
                throw IllegalStateException("AppDataBase must be initialized first. Call init(context) before getInstance().")
            }
            return instance
        }

        fun init(context: Context): AppDataBase {
            instance = Room.databaseBuilder(
                context.applicationContext,
                AppDataBase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration(BuildConfig.DEBUG)
                .build()
            return instance
        }
    }


}
