package com.paperpig.maimaidata.ui.finaletodx

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentFinaleToDxBinding
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.getInt


class FinaleToDxFragment : BaseFragment<FragmentFinaleToDxBinding>() {
    private lateinit var binding: FragmentFinaleToDxBinding


    private var tapCount = 0
    private var holdCount = 0
    private var slideCount = 0
    private var breakCount = 0
    private var dxPlayMaxScore = 0
    private var dxPlayMinScore = 0
    private var dxTotalScore = 0


    private var breakScore = 0

    private var tapPerfect = 0
    private var tapGreat = 0
    private var tapGood = 0
    private var tapMiss = 0

    private var holdPerfect = 0
    private var holdGreat = 0
    private var holdGood = 0
    private var holdMiss = 0

    private var slidePerfect = 0
    private var slideGreat = 0
    private var slideGood = 0
    private var slideMiss = 0

    private var breakPerfect = 0
    private var breakGreat = 0
    private var breakGood = 0
    private var breakMiss = 0


    companion object {
        @JvmStatic
        fun newInstance() =
            FinaleToDxFragment()

        const val TAG = "DXTransformFragment"
    }


    override fun getViewBinding(container: ViewGroup?): FragmentFinaleToDxBinding {
        binding = FragmentFinaleToDxBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        with(binding) {
            calculate.setOnClickListener {
                hideKeyboard(view)

                tapPerfect = tapPerfectEt.text.toString().getInt()
                tapGreat = tapGreatEt.text.toString().getInt()
                tapGood = tapGoodEt.text.toString().getInt()
                tapMiss = tapMissEt.text.toString().getInt()

                holdPerfect = holdPerfectEt.text.toString().getInt()
                holdGreat = holdGreatEt.text.toString().getInt()
                holdGood = holdGoodEt.text.toString().getInt()
                holdMiss = holdMissEt.text.toString().getInt()

                slidePerfect = slidePerfectEt.text.toString().getInt()
                slideGreat = slideGreatEt.text.toString().getInt()
                slideGood = slideGoodEt.text.toString().getInt()
                slideMiss = slideMissEt.text.toString().getInt()

                breakPerfect = breakPerfectEt.text.toString().getInt()
                breakGreat = breakGreatEt.text.toString().getInt()
                breakGood = breakGoodEt.text.toString().getInt()
                breakMiss = breakMissEt.text.toString().getInt()
                breakScore = breakScoreEt.text.toString().getInt()


                tapCount = tapPerfect + tapGreat + tapGood + tapMiss
                holdCount = holdPerfect + holdGreat + holdGood + holdMiss
                slideCount = slidePerfect + slideGreat + slideGood + slideMiss
                breakCount = breakPerfect + breakGreat + breakGood + breakMiss

                dxTotalScore = tapCount * 10 + holdCount * 20 + slideCount * 30 + breakCount * 50
                dxPlayMaxScore = tapPerfect * 10 + tapGreat * 8 + tapGood * 5 +
                        holdPerfect * 20 + holdGreat * 16 + holdGood * 10 +
                        slidePerfect * 30 + slideGreat * 24 + slideGood * 15 +
                        breakPerfect * 50 + breakGreat * 40 + breakGood * 20

                dxPlayMinScore = tapPerfect * 10 + tapGreat * 8 + tapGood * 5 +
                        holdPerfect * 20 + holdGreat * 16 + holdGood * 10 +
                        slidePerfect * 30 + slideGreat * 24 + slideGood * 15 +
                        breakPerfect * 50 + breakGreat * 25 + breakGood * 20

                if (breakScore == 0) {
                    val dxMaxScore =
                        dxPlayMaxScore.toFloat() / dxTotalScore * 100 + breakPerfect.toFloat() / breakCount * 1 + breakGreat.toFloat() / breakCount * 0.4 + breakGood.toFloat() / breakCount * 0.3
                    val dxMinScore =
                        dxPlayMinScore.toFloat() / dxTotalScore * 100 + breakPerfect.toFloat() / breakCount * 0.5 + breakGreat.toFloat() / breakCount * 0.4 + breakGood.toFloat() / breakCount * 0.3


                    dxScore.text = String.format(
                        getString(R.string.maimaidx_scope_achievement_desc),
                        dxMinScore,
                        dxMaxScore
                    )
                } else {
                    if (breakGreat > 0 || breakGood > 0 || breakMiss > 0 || breakPerfect * 2600 < breakScore || breakPerfect * 2500 > breakScore) {
                        Toast.makeText(context, "绝赞数据错误，请重新填写", Toast.LENGTH_SHORT).show()
                    } else {
                        val dxBreakScore =
                            1 - 0.25 * (breakCount * 2600 - breakScore) / 50 / breakCount
                        val dxCurrentScore =
                            dxPlayMaxScore.toFloat() / dxTotalScore * 100 + dxBreakScore

                        dxScore.text =
                            String.format(
                                getString(R.string.maimaidx_achievement_desc),
                                dxCurrentScore
                            )

                    }
                }

            }
        }

    }
}



