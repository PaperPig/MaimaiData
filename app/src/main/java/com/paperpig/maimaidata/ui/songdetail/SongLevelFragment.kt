package com.paperpig.maimaidata.ui.songdetail

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongLevelBinding
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.ui.BaseFragment
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private const val ARG_PARAM1 = "songData"
private const val ARG_PARAM2 = "position"

/**
 * A simple [Fragment] subclass.
 * Use the [SongLevelFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SongLevelFragment : BaseFragment<FragmentSongLevelBinding>() {
    private lateinit var binding: FragmentSongLevelBinding
    private lateinit var songData: SongData
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            songData = it.getParcelable(ARG_PARAM1)!!
            position = it.getInt(ARG_PARAM2)
        }
    }


    override fun getViewBinding(container: ViewGroup?): FragmentSongLevelBinding {
        binding =  FragmentSongLevelBinding.inflate(layoutInflater, container, false)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val note = songData.charts[position].notes


        val breakTotal = note[note.size - 1]
        val totalScore = totalScore(note, songData.type == "DX")
        val format = DecimalFormat("0.#####%")

        format.roundingMode = RoundingMode.DOWN
        binding.songLevel.text = songData.ds[position].toString()
        binding.songDesign.text = songData.charts[position].charter

        binding.notesCount.text = (songData.charts[position].notes).sum().toString()
        binding.tapCount.text = songData.charts[position].notes[0].toString()
        binding.holdCount.text = songData.charts[position].notes[1].toString()
        binding.slideCount.text = songData.charts[position].notes[2].toString()
        if (songData.type == "DX") {
            binding.touchCount.text = songData.charts[position].notes[3].toString()
            binding.breakCount.text = songData.charts[position].notes[4].toString()
        } else {
            binding.touchCount.text = "0"
            binding.breakCount.text = songData.charts[position].notes[3].toString()
        }

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
            format.format(5f / totalScore * 0.2 + (0.01 / breakTotal) * 0.6)
        binding.breakGreat3xScore.text =
            format.format(5f / totalScore * 0.4 + (0.01 / breakTotal) * 0.6)
        binding.breakGreat25xScore.text =
            format.format(5f / totalScore * 0.5 + (0.01 / breakTotal) * 0.6)
        binding.breakGoodScore.text =
            format.format(5f / totalScore * 0.6 + (0.01 / breakTotal) * 0.7)
        binding.breakMissScore.text = format.format(5f / totalScore + 0.01 / breakTotal)
        binding.break50Score.text = format.format(0.01 / breakTotal * 0.25)
        binding.break100Score.text = (format.format((0.01 / breakTotal) * 0.5))

        val bgColor =
            ((binding.songNoteLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_note_bg
            ) as GradientDrawable

        val bgStroke =
            ((binding.songNoteLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_note_stroke
            ) as GradientDrawable
        bgColor.setColor(ContextCompat.getColor(requireContext(), songData.getBgColor()))
        bgStroke.setStroke(
            5,
            ContextCompat.getColor(requireContext(), songData.getBgColor())
        )

        val noteAchievementBg =
            ((binding.noteAchievementLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.note_achievement_bg
            ) as GradientDrawable

        noteAchievementBg.setStroke(5, ContextCompat.getColor(requireContext(), songData.getBgColor()))

        if (songData.type == "DX") {
            binding.finaleAchievementLayout.visibility = View.GONE
        } else {
            binding.finaleAchievementLayout.visibility = View.VISIBLE
            binding.finaleAchievement.text =
                String.format(
                    getString(R.string.maimai_achievement_desc), BigDecimal(
                        (note[0] * 500 + note[1] * 1000 + note[2] * 1500 + note[3] * 2600) * 1.0 /
                                (note[0] * 500 + note[1] * 1000 + note[2] * 1500 + note[3] * 2500) * 100
                    ).setScale(2, BigDecimal.ROUND_DOWN)
                )
        }
    }

    private fun totalScore(note: List<Int>, isDx: Boolean): Int {
        return if (isDx) {
            (note[0] + note[3]) + note[1] * 2 + note[2] * 3 + note[4] * 5
        } else {
            note[0] + note[1] * 2 + note[2] * 3 + note[3] * 5
        }
    }

    companion object {
        fun newInstance(song: SongData, position: Int) =
            SongLevelFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PARAM1, song)
                    putInt(ARG_PARAM2, position)
                }
            }
    }
}