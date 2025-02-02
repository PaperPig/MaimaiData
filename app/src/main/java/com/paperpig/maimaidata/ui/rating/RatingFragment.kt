package com.paperpig.maimaidata.ui.rating

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentRatingBinding
import com.paperpig.maimaidata.model.Rating
import com.paperpig.maimaidata.ui.about.AboutActivity
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.checklist.LevelCheckActivity
import com.paperpig.maimaidata.ui.checklist.VersionCheckActivity
import com.paperpig.maimaidata.ui.finaletodx.FinaleToDxActivity
import com.paperpig.maimaidata.ui.maimaidxprober.LoginActivity
import com.paperpig.maimaidata.ui.maimaidxprober.ProberActivity
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.getInt
import java.util.Locale
import kotlin.math.floor

class RatingFragment : BaseFragment<FragmentRatingBinding>() {
    private lateinit var binding: FragmentRatingBinding
    private lateinit var resultAdapter: RatingResultAdapter

    companion object {
        @JvmStatic
        fun newInstance() =
            RatingFragment()

        const val TAG = "RatingFragment"
    }

    override fun getViewBinding(container: ViewGroup?): FragmentRatingBinding {
        binding = FragmentRatingBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onResume() {
        super.onResume()
        if (SharePreferencesUtils(requireContext()).getUserName().isEmpty()) {
            binding.accountText.setText(R.string.no_logged_in)
            binding.proberQueryBtn.visibility = View.GONE
            binding.proberLoginBtn.setText(R.string.login)
        } else {
            binding.accountText.text = SharePreferencesUtils(requireContext()).getUserName()
            binding.proberQueryBtn.visibility = View.VISIBLE
            binding.proberLoginBtn.setText(R.string.change_account)

        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ratingResultRecycler.apply {
            resultAdapter = RatingResultAdapter()
            layoutManager = LinearLayoutManager(context)
            adapter = resultAdapter
        }

        binding.proberLoginBtn.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.proberQueryBtn.setOnClickListener {
            startActivity(Intent(context, ProberActivity::class.java))
        }

        binding.proberLoginBtn.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.proberLevelCheckBtn.setOnClickListener {
            startActivity(Intent(context, LevelCheckActivity::class.java))
        }

        binding.proberVersionCheckBtn.setOnClickListener {
            startActivity(Intent(context, VersionCheckActivity::class.java))
        }

        binding.proberFinaleToDxBtn.setOnClickListener {
            startActivity(Intent(context, FinaleToDxActivity::class.java))
        }

        binding.calculateBtn.setOnClickListener {
            hideKeyboard(view)
            onCalculate(binding.targetRatingEdit.text.toString())
        }

        binding.calculateSingleRating.setOnClickListener {
            hideKeyboard(view)
            if (binding.inputSongLevel.text.toString()
                    .isNotEmpty() && binding.inputSongAchievement.text.toString()
                    .isNotEmpty()
            ) {
                binding.outputSingleRating.text = ConvertUtils.achievementToRating(
                    (binding.inputSongLevel.text.toString().toFloat() * 10).toInt(),
                    (binding.inputSongAchievement.text.toString().toFloat() * 10000).toInt()
                ).toString()
            } else {
                showToast()
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.about).isVisible = !isHidden
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.about_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.about -> {
                        startActivity(Intent(requireContext(), AboutActivity::class.java))
                        return true
                    }
                }
                return false
            }

        })
    }

    private fun showToast() {
        val text = "请输入内容!"
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(this.context, text, duration)
        toast.show()
    }


    private fun onCalculate(targetString: String) {
        val targetRating = targetString.getInt()
        if (targetRating <= 0) {
            showToast()
            return
        }
        val rating = targetRating / 50

        val minLv = getReachableLevel(rating)
        val map = mutableMapOf<Int, Int>()
        val list = mutableListOf<Rating>()

        for (i in 150 downTo minLv) {

            when (val reachableAchievement = getReachableAchievement(i, rating)) {
                800000, 900000, 940000 ->
                    map[reachableAchievement] = i

                in 970000..1010000 ->
                    map[reachableAchievement] = i
            }
        }

        map.forEach {
            list.add(
                Rating(
                    (it.value / 10f),
                    String.format(Locale.getDefault(), "%.4f%%", it.key / 10000f),
                    ConvertUtils.achievementToRating(it.value, it.key),
                    ConvertUtils.achievementToRating(it.value, it.key) * 50
                )
            )
        }

        resultAdapter.setData(list)

    }
}


private fun getReachableLevel(rating: Int): Int {
    for (i in 10..150) {
        if (rating < ConvertUtils.achievementToRating(i, 1005000)) {
            return i
        }
    }
    return 0
}

private fun getReachableAchievement(level: Int, rating: Int): Int {
    var maxAchi = 1010000
    var minAchi = 0
    var tempAchi: Int


    if (ConvertUtils.achievementToRating(level, 1005000) < rating)
        return 1010001
    for (n in 0..20) {
        if (maxAchi - minAchi >= 2) {
            tempAchi = floor((maxAchi.toDouble() + minAchi) / 2).toInt()

            if (ConvertUtils.achievementToRating(level, tempAchi) < rating)
                minAchi = tempAchi
            else maxAchi = tempAchi
        }
    }
    return maxAchi
}
