package com.paperpig.maimaidata.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.paperpig.maimaidata.model.PlayerProfile
import com.paperpig.maimaidata.model.PlayerTrophy

/**
 * App 本地玩家档案数据库实体（单行缓存，id 固定为 1）。
 */
@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey
    val id: Int = SINGLE_PROFILE_ID,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "rating")
    val rating: Int,

    @ColumnInfo(name = "friend_code")
    val friendCode: Long,

    @ColumnInfo(name = "course_rank")
    val courseRank: Int,

    @ColumnInfo(name = "class_rank")
    val classRank: Int,

    @ColumnInfo(name = "star")
    val star: Int,

    @ColumnInfo(name = "trophy_name")
    val trophyName: String?,

    @ColumnInfo(name = "trophy_color")
    val trophyColor: String?,

    @ColumnInfo(name = "icon_url")
    val iconUrl: String?,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
) {
    fun toModel(): PlayerProfile = PlayerProfile(
        name = name,
        rating = rating,
        friendCode = friendCode,
        courseRank = courseRank,
        classRank = classRank,
        star = star,
        trophy = trophyName?.takeIf { it.isNotBlank() }?.let {
            PlayerTrophy(
                name = it,
                color = trophyColor
            )
        },
        iconUrl = iconUrl,
        updatedAt = updatedAt
    )

    companion object {
        const val SINGLE_PROFILE_ID = 1

        fun fromModel(profile: PlayerProfile): PlayerProfileEntity = PlayerProfileEntity(
            id = SINGLE_PROFILE_ID,
            name = profile.name,
            rating = profile.rating,
            friendCode = profile.friendCode,
            courseRank = profile.courseRank,
            classRank = profile.classRank,
            star = profile.star,
            trophyName = profile.trophy?.name,
            trophyColor = profile.trophy?.color,
            iconUrl = profile.iconUrl,
            updatedAt = profile.updatedAt
        )
    }
}
