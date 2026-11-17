package com.paperpig.maimaidata.network.lxns

import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.ConvertUtils
import kotlin.math.floor

/**
 * 将落雪查分器返回的成绩（LxnsScore）格式化并映射为本地数据库 RecordEntity。
 */
object LxnsRecordConverter {

    fun convert(
        lxnsScores: List<LxnsScore>,
        localSongs: List<SongWithChartsEntity>
    ): List<RecordEntity> {
        val index = LocalSongIndex.from(localSongs)
        return lxnsScores.map { score -> convertSingle(score, index) }
    }

    private fun convertSingle(score: LxnsScore, index: LocalSongIndex): RecordEntity {
        val isUtageInput = score.type == "utage" || score.id >= UTAGE_ID_THRESHOLD
        val mappedType = when (score.type) {
            "dx" -> Constants.CHART_TYPE_DX
            "standard" -> Constants.CHART_TYPE_SD
            else -> Constants.CHART_TYPE_DX
        }

        val matchedSong = if (isUtageInput) {
            index.findUtage(score.id, score.songName)
        } else {
            index.findNormal(mappedType, score.id, score.songName)
        }

        val isUtage = isUtageInput || matchedSong?.songData?.genre == Constants.GENRE_UTAGE
        val finalType = matchedSong?.songData?.type ?: mappedType
        val finalSongId = matchedSong?.songData?.id ?: score.id
        val finalTitle = matchedSong?.songData?.title ?: score.songName

        val matchedChart = matchedSong?.let { findChart(it, score.levelIndex, isUtage) }
        val ds = matchedChart?.ds ?: 0.0
        val fc = score.fc.orEmpty()
        val fs = score.fs.orEmpty()
        val isAp = fc == "ap" || fc == "app"

        val ra = when {
            isUtage -> 0
            ds > 0.0 -> ConvertUtils.achievementToRating(
                Math.round(ds * 10).toInt(),
                Math.round(score.achievements * 10000).toInt(),
                isAp
            )
            else -> floor(score.dxRating).toInt()
        }

        return RecordEntity(
            achievements = score.achievements,
            ds = ds,
            dxScore = score.dxScore,
            fc = fc,
            fs = fs,
            level = score.level.ifBlank { matchedChart?.level.orEmpty() },
            levelIndex = score.levelIndex,
            levelLabel = toLevelLabel(score.levelIndex, isUtage),
            ra = ra,
            rate = score.rate.ifBlank { computeRate(score.achievements) },
            songId = finalSongId,
            title = finalTitle,
            type = finalType
        )
    }

    private fun findChart(
        song: SongWithChartsEntity,
        levelIndex: Int,
        isUtage: Boolean
    ): ChartEntity? {
        if (isUtage) {
            return song.charts.firstOrNull { it.difficultyType == DifficultyType.UTAGE }
                ?: song.charts.firstOrNull()
        }
        val targetDiff = when (levelIndex) {
            0 -> DifficultyType.BASIC
            1 -> DifficultyType.ADVANCED
            2 -> DifficultyType.EXPERT
            3 -> DifficultyType.MASTER
            4 -> DifficultyType.REMASTER
            else -> DifficultyType.UNKNOWN
        }
        return song.charts.firstOrNull { it.difficultyType == targetDiff }
    }

    private fun toLevelLabel(levelIndex: Int, isUtage: Boolean): String {
        if (isUtage) return "U·TA·GE"
        return when (levelIndex) {
            0 -> "Basic"
            1 -> "Advanced"
            2 -> "Expert"
            3 -> "Master"
            4 -> "Re:MASTER"
            else -> ""
        }
    }

    private fun computeRate(achievements: Double): String {
        return when {
            achievements >= 100.5 -> "sssp"
            achievements >= 100.0 -> "sss"
            achievements >= 99.5 -> "ssp"
            achievements >= 99.0 -> "ss"
            achievements >= 98.0 -> "sp"
            achievements >= 97.0 -> "s"
            achievements >= 94.0 -> "aaa"
            achievements >= 90.0 -> "aa"
            achievements >= 80.0 -> "a"
            achievements >= 75.0 -> "bbb"
            achievements >= 70.0 -> "bb"
            achievements >= 60.0 -> "b"
            achievements >= 50.0 -> "c"
            else -> "d"
        }
    }

    private class LocalSongIndex(
        private val byTypeAndExactId: Map<Pair<String, Int>, SongWithChartsEntity>,
        private val byTypeAndBaseId: Map<Pair<String, Int>, List<SongWithChartsEntity>>,
        private val byTypeAndTitle: Map<Pair<String, String>, List<SongWithChartsEntity>>,
        private val byTypeAndLowerTitle: Map<Pair<String, String>, List<SongWithChartsEntity>>,
        private val utageById: Map<Int, SongWithChartsEntity>,
        private val utageByTitle: Map<String, List<SongWithChartsEntity>>
    ) {
        fun findNormal(type: String, lxnsId: Int, songName: String): SongWithChartsEntity? {
            byTypeAndExactId[type to lxnsId]?.let { return it }

            val baseId = lxnsId % 10000
            val baseCandidates = byTypeAndBaseId[type to baseId].orEmpty()
            if (baseCandidates.size == 1) {
                return baseCandidates.first()
            }
            if (baseCandidates.size > 1) {
                baseCandidates.firstOrNull { it.songData.title == songName }?.let { return it }
                baseCandidates.firstOrNull { it.songData.title.equals(songName, ignoreCase = true) }
                    ?.let { return it }
            }

            byTypeAndTitle[type to songName]?.firstOrNull()?.let { return it }
            return byTypeAndLowerTitle[type to songName.lowercase()]?.firstOrNull()
        }

        fun findUtage(lxnsId: Int, songName: String): SongWithChartsEntity? {
            utageById[lxnsId]?.let { return it }
            return utageByTitle[songName]?.firstOrNull()
        }

        companion object {
            fun from(localSongs: List<SongWithChartsEntity>): LocalSongIndex {
                val normalSongs = localSongs.filter { it.songData.genre != Constants.GENRE_UTAGE }
                val utageSongs = localSongs.filter { it.songData.genre == Constants.GENRE_UTAGE }

                return LocalSongIndex(
                    byTypeAndExactId = normalSongs.associateBy { it.songData.type to it.songData.id },
                    byTypeAndBaseId = normalSongs.groupBy { it.songData.type to (it.songData.id % 10000) },
                    byTypeAndTitle = normalSongs.groupBy { it.songData.type to it.songData.title },
                    byTypeAndLowerTitle = normalSongs.groupBy {
                        it.songData.type to it.songData.title.lowercase()
                    },
                    utageById = utageSongs.associateBy { it.songData.id },
                    utageByTitle = utageSongs.groupBy { it.songData.title }
                )
            }
        }
    }

    private const val UTAGE_ID_THRESHOLD = 100000
}
