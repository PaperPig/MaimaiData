package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.model.DifficultyType
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "chart",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ChartEntity(
    // 主键（自增长，默认值 0）
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // 外键关联 SongDataEntity.id
    @ColumnInfo(name = "song_id", index = true) // 添加索引提升查询性能
    val songId: String,

    // 难度类型（如BASIC/ADVANCED/EXPERT/MASTER/REMASTER）
    @ColumnInfo(name = "difficulty_type")
    val difficultyType: DifficultyType,

    // 标准 or DX
    val type: String,

    // 当前定数
    val ds: Double,

    // 旧版本定数
    @ColumnInfo(name = "old_ds")
    val oldDs: Double?,

    // 难度等级（如 "12"）
    val level: String,

    // 谱面作者
    val charter: String,

    // 音符统计（显式指定列名）
    @ColumnInfo(name = "notes_tap")
    val notesTap: Int,

    @ColumnInfo(name = "notes_hold")
    val notesHold: Int,

    @ColumnInfo(name = "notes_slide")
    val notesSlide: Int,

    @ColumnInfo(name = "notes_touch")
    val notesTouch: Int,

    @ColumnInfo(name = "notes_break")
    val notesBreak: Int,

    @ColumnInfo(name = "notes_total")
    val notesTotal: Int
) : Parcelable

