package com.paperpig.maimaidata.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@Entity(
    tableName = "chart_stats",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"]
    )]
)
data class ChartStatsEntity(
    // 主键（自增长，默认值 0）
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // JSON 的 key
    @ColumnInfo(name = "song_id", index = true)
    val songId: Int,

    // 样本数量
    val cnt: Double?,

    // 谱面的官标难度等级
    val diff: String?,

    // 等级索引
    @ColumnInfo(name = "level_index")
    val levelIndex: Int,

    // 谱面的拟合难度
    @ColumnInfo(name = "fit_diff")
    val fitDiff: Double?,

    // 谱面平均达成率
    val avg: Double?,

    // 谱面平均 DX Scores
    @ColumnInfo(name = "avg_dx")
    val avgDx: Double?,

    // 谱面达成率的标准差
    @ColumnInfo(name = "std_dev")
    val stdDev: Double?,

    // 评级分布
    @TypeConverters(ListIntConverter::class)
    val dist: List<Int>?,

    // Full Combo 分布
    @ColumnInfo(name = "fc_dist")
    @TypeConverters(ListIntConverter::class)
    val fcDist: List<Int>?
)

class ListIntConverter {
    @TypeConverter
    fun fromList(value: List<Int>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toList(value: String?): List<Int>? {
        return Gson().fromJson(value, object : TypeToken<List<Int>>() {}.type)
    }
}