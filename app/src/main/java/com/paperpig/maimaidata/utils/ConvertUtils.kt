package com.paperpig.maimaidata.utils

object ConvertUtils {
    /**
     * 通过定数和达成率计算单曲rating
     */
    fun achievementToRating(level: Int, achi: Int): Int {
        val multiplier = when (achi) {
            in 1005000..Int.MAX_VALUE -> 22.4
            1004999 -> 22.2
            in 1000000..1004998 -> 21.6
            999999 -> 21.4
            in 995000..999998 -> 21.1
            in 990000..994999 -> 20.8
            989999 -> 20.6
            in 980000..989998 -> 20.3
            in 970000..979999 -> 20.0
            969999 -> 17.6
            in 940000..969998 -> 16.8
            in 900000..939999 -> 15.2
            in 800000..899999 -> 13.6
            799999 -> 12.8
            in 750000..799998 -> 12.0
            in 700000..749999 -> 11.2
            in 600000..699999 -> 9.6
            in 500000..599999 -> 8.0
            in 400000..499999 -> 6.4
            in 300000..399999 -> 4.8
            in 200000..299999 -> 3.2
            in 100000..199999 -> 1.6
            else -> 0.0
        }
        return (achi.coerceAtMost(1005000) * level * multiplier / 10000000).toInt()
    }
}
