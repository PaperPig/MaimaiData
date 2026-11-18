package com.paperpig.maimaidata.model

/**
 * App 本地的舞萌 DX 玩家个人档案模型（独立于第三方查分器，由本地同步页面解析生成）。
 */
data class PlayerProfile(
    val name: String = "",
    val rating: Int = 0,
    val friendCode: Long = 0L,
    val courseRank: Int = 0,
    val classRank: Int = 0,
    val star: Int = 0,
    val trophy: PlayerTrophy? = null,
    val iconUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 玩家称号信息。
 */
data class PlayerTrophy(
    val name: String = "",
    val color: String? = null
)

/**
 * 玩家展示辅助（段位与友人对战阶级名称映射）。
 */
object PlayerProfileFormatter {
    fun formatCourseRank(courseRank: Int): String = when (courseRank) {
        0 -> "初学者"
        1 -> "初段"
        2 -> "二段"
        3 -> "三段"
        4 -> "四段"
        5 -> "五段"
        6 -> "六段"
        7 -> "七段"
        8 -> "八段"
        9 -> "九段"
        10 -> "十段"
        11 -> "真传"
        12 -> "真初段"
        13 -> "真二段"
        14 -> "真三段"
        15 -> "真四段"
        16 -> "真五段"
        17 -> "真六段"
        18 -> "真七段"
        19 -> "真八段"
        20 -> "真九段"
        21 -> "真十段"
        22 -> "真皆传"
        23 -> "里皆传"
        else -> "段位 $courseRank"
    }

    fun formatClassRank(classRank: Int): String = when (classRank) {
        in 0..4 -> "B${5 - classRank}"
        in 5..9 -> "A${10 - classRank}"
        in 10..14 -> "S${15 - classRank}"
        in 15..19 -> "SS${20 - classRank}"
        in 20..24 -> "SSS${25 - classRank}"
        25 -> "LEGEND"
        else -> "C$classRank"
    }
}
