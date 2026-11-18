package com.paperpig.maimaidata.ui.rating

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityRatingCalculatorBinding
import com.paperpig.maimaidata.model.Rating
import com.paperpig.maimaidata.ui.BaseActivity
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.getInt
import java.util.Locale
import kotlin.math.floor

/**
 * 独立 Rating 计算工具页：支持目标 Rating 推荐配置与单曲 Rating 实时测算。
 */
class RatingCalculatorActivity : BaseActivity<ActivityRatingCalculatorBinding>() {

    override val includeImeInBottomInset: Boolean = true

    private lateinit var resultAdapter: RatingResultAdapter

    override fun inflateBinding() = ActivityRatingCalculatorBinding.inflate(layoutInflater)

    override fun topInsetTargetView(): View = binding.toolbarLayout.toolbar

    override fun bottomInsetTargetView(): View = binding.ratingCalculatorScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setTitle(R.string.rating_calculator)
        }

        resultAdapter = RatingResultAdapter()
        binding.ratingResultRecycler.apply {
            layoutManager = LinearLayoutManager(this@RatingCalculatorActivity)
            adapter = resultAdapter
        }

        binding.calculateBtn.setOnClickListener { view ->
            hideKeyboard(view)
            onCalculate(binding.targetRatingEdit.text.toString())
        }

        val singleRatingTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                updateSingleRating()
            }
        }
        binding.inputSongLevel.addTextChangedListener(singleRatingTextWatcher)
        binding.inputSongAchievement.addTextChangedListener(singleRatingTextWatcher)
        binding.inputSongAp.setOnCheckedChangeListener { _, _ -> updateSingleRating() }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
        if (imm is InputMethodManager) {
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun onCalculate(targetString: String) {
        val targetRating = targetString.getInt()
        if (targetRating <= 0) {
            Toast.makeText(this, "请输入内容!", Toast.LENGTH_SHORT).show()
            return
        }
        val rating = targetRating / 50

        val minLv = getReachableLevel(rating)
        val map = mutableMapOf<Int, Int>()
        val list = mutableListOf<Rating>()

        for (i in 150 downTo minLv) {
            when (val reachableAchievement = getReachableAchievement(i, rating)) {
                800000, 900000, 940000,
                in 970000..1010000 -> map[reachableAchievement] = i
            }
        }

        map.forEach { (achi, level) ->
            val single = ConvertUtils.achievementToRating(level, achi)
            list.add(
                Rating(
                    level / 10f,
                    String.format(Locale.getDefault(), "%.4f%%", achi / 10000f),
                    single,
                    single * 50
                )
            )
        }

        resultAdapter.setData(list)
    }

    private fun updateSingleRating() {
        val level = binding.inputSongLevel.text.toString().toDoubleOrNull()
        val achievement = binding.inputSongAchievement.text.toString().toDoubleOrNull()
        binding.outputSingleRating.text = if (level != null && achievement != null) {
            ConvertUtils.achievementToRating(
                Math.round(level * 10).toInt(),
                Math.round(achievement * 10000).toInt(),
                isAp = binding.inputSongAp.isChecked
            ).toString()
        } else {
            "0"
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

        if (ConvertUtils.achievementToRating(level, 1005000) < rating) {
            return 1010001
        }
        for (n in 0..20) {
            if (maxAchi - minAchi >= 2) {
                tempAchi = floor((maxAchi.toDouble() + minAchi) / 2).toInt()
                if (ConvertUtils.achievementToRating(level, tempAchi) < rating) {
                    minAchi = tempAchi
                } else {
                    maxAchi = tempAchi
                }
            }
        }
        return maxAchi
    }
}
