package com.paperpig.maimaidata.utils

import com.paperpig.maimaidata.db.entity.AliasEntity
import com.paperpig.maimaidata.db.entity.ChartEntity
import com.paperpig.maimaidata.db.entity.SongDataEntity
import com.paperpig.maimaidata.model.DifficultyType
import com.paperpig.maimaidata.model.SongData

object JsonConvertToDb {
    fun convert(list: List<SongData>): ConversionResult {
        val songList = list.map { song ->
            SongDataEntity(
                song.id.toInt(),
                song.title,
                song.title,
                song.basic_info.artist,
                song.basic_info.image_url,
                song.basic_info.genre,
                song.basic_info.genre,
                song.basic_info.bpm,
                song.basic_info.from,
                song.type,
                song.basic_info.version,
                song.basic_info.is_new,
                song.basic_info.kanji,
                song.basic_info.comment,
                song.basic_info.buddy
            )
        }

        val chartList = list.flatMap { song ->
            song.charts.mapIndexed { i, chart ->
                val difficultyType = getDifficultyType(song.basic_info.genre, i)

                val notes = chart.notes
                val totalNotes = notes.sum()

                val (note1, note2, note3, note4, note5) = when (song.type) {
                    Constants.CHART_TYPE_SD -> listOf(notes[0], notes[1], notes[2], 0, notes[3])
                    else -> listOf(notes[0], notes[1], notes[2], notes[3], notes[4])
                }

                ChartEntity(
                    0,
                    song.id.toInt(),
                    difficultyType,
                    song.type,
                    song.ds[i],
                    song.old_ds.getOrNull(i),
                    song.level[i],
                    chart.charter,
                    note1,
                    note2,
                    note3,
                    note4,
                    note5,
                    totalNotes
                )
            }
        }


        val aliasList = list.flatMap { song ->
            song.alias?.map { alias ->
                AliasEntity(0, song.id.toInt(), alias)
            } ?: emptyList()
        }

        return ConversionResult(songList, chartList, aliasList)
    }

    private fun getDifficultyType(genre: String, index: Int): DifficultyType {
        return if (genre == Constants.GENRE_UTAGE) {
            when (index) {
                0 -> DifficultyType.UTAGE
                1 -> DifficultyType.UTAGE_PLAYER2
                else -> DifficultyType.UNKNOWN
            }
        } else {
            when (index) {
                0 -> DifficultyType.BASIC
                1 -> DifficultyType.ADVANCED
                2 -> DifficultyType.EXPERT
                3 -> DifficultyType.MASTER
                4 -> DifficultyType.REMASTER
                else -> DifficultyType.UNKNOWN
            }
        }
    }

    data class ConversionResult(
        val songs: List<SongDataEntity>,
        val charts: List<ChartEntity>,
        val aliases: List<AliasEntity>
    )
}