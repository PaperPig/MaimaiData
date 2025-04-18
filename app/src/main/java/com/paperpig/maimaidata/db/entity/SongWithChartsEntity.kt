package com.paperpig.maimaidata.db.entity

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.parcelize.Parcelize


@Parcelize
data class SongWithChartsEntity(
    @Embedded val songData: SongDataEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "song_id"
    )
    val charts: List<ChartEntity>
) : Parcelable
