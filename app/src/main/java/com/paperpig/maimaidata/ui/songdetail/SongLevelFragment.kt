package com.paperpig.maimaidata.ui.songdetail

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.model.totalScore
import kotlinx.android.synthetic.main.fragment_song_level.*
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
class SongLevelFragment : Fragment() {
    private lateinit var songData: SongData
    private var position: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            songData = it.getSerializable(ARG_PARAM1) as SongData
            position = it.getSerializable(ARG_PARAM2) as Int
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_song_level, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val note = songData.charts[position].notes


        val breakTotal = note[note.size - 1]
        val totalScore = totalScore(note, songData.type == "DX")
        val format = DecimalFormat("0.#####%")

        format.roundingMode = RoundingMode.DOWN
        songLevel.text = songData.ds[position].toString()
        songDesign.text = songData.charts[position].charter

        notesCount.text = (songData.charts[position].notes).sum().toString()
        tapCount.text = songData.charts[position].notes[0].toString()
        holdCount.text = songData.charts[position].notes[1].toString()
        slideCount.text = songData.charts[position].notes[2].toString()
        if (songData.type=="DX"){
            touchCount.text = songData.charts[position].notes[3].toString()
            breakCount.text = songData.charts[position].notes[4].toString()
        }else{
            touchCount.text = "0"
            breakCount.text = songData.charts[position].notes[3].toString()
        }

        tapGreatScore.text = format.format(1f / totalScore * 0.2)
        tapGoodScore.text = format.format(1f / totalScore * 0.5)
        tapMissScore.text = format.format(1f / totalScore)
        holdGreatScore.text = format.format(2f / totalScore * 0.2)
        holdGoodScore.text = format.format(2f / totalScore * 0.5)
        holdMissScore.text = format.format(2f / totalScore)
        slideGreatScore.text = format.format(3f / totalScore * 0.2)
        slideGoodScore.text = format.format(3f / totalScore * 0.5)
        slideMissScore.text = format.format(3f / totalScore)
        breakGreatScore.text = format.format(5f / totalScore * 0.2 + (0.01 / breakTotal) * 0.6)
        breakGoodScore.text = format.format(5f / totalScore * 0.5 + (0.01 / breakTotal) * 0.7)
        breakMissScore.text = format.format(5f / totalScore + 0.01 / breakTotal)
        break50Score.text = format.format(0.01 / breakTotal * 0.25)
        break100Score.text = (format.format((0.01 / breakTotal) * 0.5))

        val bgColor =
            ((songNoteLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_note_bg
            ) as GradientDrawable

        val bgStroke =
            ((songNoteLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.song_note_stroke
            ) as GradientDrawable
        bgColor.setColor(ContextCompat.getColor(context!!, songData.getBgColor()))
        bgStroke.setStroke(
            5,
            ContextCompat.getColor(context!!, songData.getBgColor())
        )

        val noteAchievementBg =
            ((noteAchievementLayout.background as LayerDrawable).getDrawable(0) as LayerDrawable).findDrawableByLayerId(
                R.id.note_achievement_bg
            ) as GradientDrawable

        noteAchievementBg.setStroke(5, ContextCompat.getColor(context!!, songData.getBgColor()))

        if (songData.type == "DX") {
            finaleAchievementLayout.visibility = View.GONE
        } else {
            finaleAchievementLayout.visibility = View.VISIBLE
            finaleAchievement.text =
                String.format(
                    getString(R.string.maimai_achievement_desc), BigDecimal(
                        (note[0] * 500 + note[1] * 1000 + note[2] * 1500 + note[3] * 2600) * 1.0 /
                                (note[0] * 500 + note[1] * 1000 + note[2] * 1500 + note[3] * 2500) * 100
                    ).setScale(2, BigDecimal.ROUND_DOWN)
                )
        }
    }

    companion object {
        fun newInstance(song: SongData, position: Int) =
            SongLevelFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_PARAM1, song)
                    putSerializable(ARG_PARAM2, position)
                }
            }
    }
}