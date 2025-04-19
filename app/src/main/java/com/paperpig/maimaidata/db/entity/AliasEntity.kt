package com.paperpig.maimaidata.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey


@Entity(
    tableName = "alias",
    foreignKeys = [ForeignKey(
        entity = SongDataEntity::class,
        parentColumns = ["id"],
        childColumns = ["song_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AliasEntity(
    // 主键（自增长，默认值 0）
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 外键关联 SongDataEntity.id
    @ColumnInfo(name = "song_id", index = true) // 添加索引提升查询性能
    val songId: Int,

    // 别名信息
    val alias: String
)
