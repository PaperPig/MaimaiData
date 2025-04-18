package com.paperpig.maimaidata.ui.songdetail

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongLevelBinding
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.repository.ChartStatsManager
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.setCopyOnLongClick
import com.paperpig.maimaidata.utils.setShrinkOnTouch
import com.paperpig.maimaidata.utils.toDp
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private const val ARG_SONG_DATA = "song_data"
private const val ARG_POSITION = "position"
private const val ARG_RECORD = "record"


class SongLevelFragment : BaseFragment<FragmentSongLevelBinding>() {
    private lateinit var binding: FragmentSongLevelBinding
    private lateinit var data: SongWithChartsEntity
    private var record: Record? = null
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            data = it.getParcelable<SongWithChartsEntity>(ARG_SONG_DATA)!!
            position = it.getInt(ARG_POSITION)
            record = it.getParcelable<Record>(ARG_RECORD)
        }
    }


    override fun getViewBinding(container: ViewGroup?): FragmentSongLevelBinding {
        binding = FragmentSongLevelBinding.inflate(layoutInflater, container, false)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (record != null) {
            binding.chartStatusGroup.visibility = View.VISIBLE
            binding.chartNoStatusGroup.visibility = View.GONE
            binding.chartAchievement.text =
                getString(R.string.maimaidx_achievement_desc, record!!.achievements)
            binding.chartRank.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), record!!.getRankIcon())
            )
            binding.chartFcap.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), record!!.getFcIcon())
            )
            binding.chartFsfsd.setImageDrawable(
                ContextCompat.getDrawable(requireContext(), record!!.getFsIcon())
            )
        } else {
            binding.chartStatusGroup.visibility = View.GONE
            binding.chartNoStatusGroup.visibility = View.VISIBLE

            binding.recordTips.setOnClickListener {
                Toast.makeText(context, R.string.no_record_tips, Toast.LENGTH_LONG).show()
            }
        }
        val chart = data.charts[position]
        val songData = data.songData
        val statsList = ChartStatsManager.list
        val fitDiff =
        //宴会场不显示拟合定数
            //没有拟合定数数据显示为"-"
            if (chart.difficultyType.name == Constants.GENRE_UTAGE) {
                "-"
            } else {
                statsList[chart.songId]?.get(position)?.fitDiff?.let {
                    BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toString()
                } ?: "-"
            }

        binding.songFitDiff.text = fitDiff


        val totalScore =
            (chart.notesTap + chart.notesTouch) + chart.notesHold * 2 + chart.notesSlide * 3 + chart.notesBreak * 5
        val format = DecimalFormat("0.#####%")
        format.roundingMode = RoundingMode.DOWN

        chart.oldDs?.let {
            if (it < chart.ds) {
                binding.songLevel.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.mmd_color_red
                    )
                )
                binding.songLevel.text = getString(R.string.inner_level_up, chart.ds)
                binding.oldLevel.text =
                    getString(R.string.inner_level_old, it)
            } else if (it > chart.ds) {
                binding.songLevel.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.mmd_color_green
                    )
                )
                binding.songLevel.text = getString(R.string.inner_level_down, chart.ds)
                binding.oldLevel.text =
                    getString(R.string.inner_level_old, it)
            } else {
                binding.songLevel.text = "${chart.ds}"
                binding.oldLevel.text =
                    getString(R.string.inner_level_old, it)
            }
        } ?: run {
            binding.songLevel.text = chart.ds.toString()
        }

        binding.chartDesigner.apply {
            text = songData.charts[position].charter

            setShrinkOnTouch()
            setCopyOnLongClick(songData.charts[position].charter)
        }

        binding.chartView.setMaxValues((MaimaiDataApplication.instance.maxNotesStats?.let {
            listOf(
                it.total,
                it.tap,
                it.hold,
                it.slide,
                it.touch,
                it.break_
            )
        }) ?: emptyList())
        val noteValueList = listOf(
            chart.notesTotal,
            chart.notesTap,
            chart.notesHold,
            chart.notesSlide,
            chart.notesTouch,
            chart.notesBreak
        )
        binding.chartView.setValues(noteValueList)

        binding.chartView.setBarColor(songData.bgColor)

        binding.tapGreatScore.text = format.format(1f / totalScore * 0.2)
        binding.tapGoodScore.text = format.format(1f / totalScore * 0.5)
        binding.tapMissScore.text = format.format(1f / totalScore)
        binding.holdGreatScore.text = format.format(2f / totalScore * 0.2)
        binding.holdGoodScore.text = format.format(2f / totalScore * 0.5)
        binding.holdMissScore.text = format.format(2f / totalScore)
        binding.slideGreatScore.text = format.format(3f / totalScore * 0.2)
        binding.slideGoodScore.text = format.format(3f / totalScore * 0.5)
        binding.slideMissScore.text = format.format(3f / totalScore)
        binding.breakGreat4xScore.text =
            format.format(5f / totalScore * 0.2 + (0.01 / chart.notesBreak) * 0.6)
        binding.breakGreat3xScore.text =
            format.format(5f / totalScore * 0.4 + (0.01 / chart.notesBreak) * 0.6)
        binding.breakGreat25xScore.text =
            format.format(5f / totalScore * 0.5 + (0.01 / chart.notesBreak) * 0.6)
        binding.breakGoodScore.text =
            format.format(5f / totalScore * 0.6 + (0.01 / chart.notesBreak) * 0.7)
        binding.breakMissScore.text = format.format(5f / totalScore + 0.01 / chart.notesBreak)
        binding.break50Score.text = format.format(0.01 / chart.notesBreak * 0.25)
        binding.break100Score.text = (format.format((0.01 / chart.notesBreak) * 0.5))


        val notesAchievementStoke =
            (binding.noteAchievementLayout.background as LayerDrawable).findDrawableByLayerId(
                R.id.note_achievement_stroke
            ) as GradientDrawable
        val notesAchievementInnerStoke =
            (binding.noteAchievementLayout.background as LayerDrawable).findDrawableByLayerId(
                R.id.note_achievement_inner_stroke
            ) as GradientDrawable

        notesAchievementStoke.setStroke(
            4.toDp().toInt(),
            ContextCompat.getColor(requireContext(), songData.strokeColor)
        )

        notesAchievementInnerStoke.setStroke(
            3.toDp().toInt(), ContextCompat.getColor(
                requireContext(),
                songData.bgColor
            )
        )

        if (chart.type == Constants.CHART_TYPE_DX) {
            binding.finaleGroup.visibility = View.GONE
        } else {
            binding.finaleGroup.visibility = View.VISIBLE
            binding.finaleAchievement.text =
                String.format(
                    getString(R.string.maimai_achievement_format), BigDecimal(
                        (chart.notesTap * 500 + chart.notesHold * 1000 + chart.notesSlide * 1500 + chart.notesBreak * 2600) * 1.0 /
                                (chart.notesTap * 500 + chart.notesHold * 1000 + chart.notesSlide * 1500 + chart.notesBreak * 2500) * 100
                    ).setScale(2, RoundingMode.DOWN)
                )
        }
    }


    companion object {
        fun newInstance(chart: SongWithChartsEntity, position: Int, record: Record?) =
            SongLevelFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SONG_DATA, chart)
                    putInt(ARG_POSITION, position)
                    putParcelable(ARG_RECORD, record)
                }
            }
    }
}