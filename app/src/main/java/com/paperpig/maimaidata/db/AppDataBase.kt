package com.paperpig.maimaidata.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.db.AppDataBase.Companion.DATABASE_VERSION
import com.paperpig.maimaidata.db.dao.AliasDao
import com.paperpig.maimaidata.db.dao.ChartDao
import com.paperpig.maimaidata.db.dao.ChartStatsDao
import com.paperpig.maimaidata.db.dao.RecordDao
import com.paperpig.maimaidata.db.dao.SongDao
import com.paperpig.maimaidata.db.dao.SongWithChartsDao
import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.ChartStatsEntity
import com.paperpig.maimaidata.db.entity.ListIntConverter
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.utils.SpUtil

@Database(
    entities = [SongDataEntity::class, ChartEntity::class, AliasEntity::class, RecordEntity::class, ChartStatsEntity::class],
    version = DATABASE_VERSION,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = AppDataBase.ReadingMigration::class)]
)
@TypeConverters(ListIntConverter::class)
abstract class AppDataBase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun chartDao(): ChartDao
    abstract fun songWithChartDao(): SongWithChartsDao
    abstract fun aliasDao(): AliasDao
    abstract fun recordDao(): RecordDao
    abstract fun chartStatsDao(): ChartStatsDao

    // 自动迁移新增字段后，为已有歌曲补齐朗读内容。
    class ReadingMigration : AutoMigrationSpec {
        // 使用原始曲名初始化读音，避免升级后出现空白朗读。
        override fun onPostMigrate(connection: SQLiteConnection) {
            connection.execSQL("UPDATE song_data SET reading = title")
        }
    }

    companion object {
        const val DATABASE_VERSION = 2
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
                .addCallback(object : Callback() {
                    override fun onDestructiveMigration(connection: SQLiteConnection) {
                        SpUtil.setDataVersion("0")
                    }
                })
                .build()
            return instance
        }
    }


}
