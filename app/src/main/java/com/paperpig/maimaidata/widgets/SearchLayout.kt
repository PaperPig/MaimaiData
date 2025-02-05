package com.paperpig.maimaidata.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.LayoutSongSearchBinding
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.repository.SongDataManager
import java.math.BigDecimal
import java.math.RoundingMode


class SearchLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs),
    Slider.OnSliderTouchListener {
    private var listener: OnSearchResultListener? = null
    private val binding: LayoutSongSearchBinding =
        LayoutSongSearchBinding.inflate(LayoutInflater.from(context), this, true)

    private val levels = resources.getStringArray(R.array.dxp_song_level)

    private var searchLevelDs = 1.0f
    private var searchLevelString = ""

    private var genreCheckBoxList = mutableListOf(
        binding.genrePopCheckbox,
        binding.genreNicoCheckbox,
        binding.genreTouhouCheckbox,
        binding.genreVarietyCheckbox,
        binding.genreMaimaiCheckbox,
        binding.genreChuniCheckbox,
        binding.genreUtageCheckbox
    )
    private var versionCheckBoxList = mutableListOf(
        binding.versionMaimaiCheckbox,
        binding.versionGreenCheckbox,
        binding.versionOrangeCheckbox,
        binding.versionPinkCheckbox,
        binding.versionMurasakiCheckbox,
        binding.versionMilkCheckbox,
        binding.versionFinaleCheckbox,
        binding.versionDxCheckbox,
        binding.versionDx2021Checkbox,
        binding.versionDx2022Checkbox,
        binding.versionDx2023Checkbox,
        binding.versionDx2024Checkbox
    )


    init {
        searchLevelString = levels[0]
        binding.levelText.text = context.getString(R.string.search_level_string, searchLevelString)

        binding.levelSlider.apply {
            setLabelFormatter { value ->
                val index = value.toInt()
                searchLevelString = levels.getOrNull(index) ?: "UNKNOWN"
                context.getString(R.string.search_level_string, searchLevelString)
            }
            addOnSliderTouchListener(this@SearchLayout)
        }

        binding.levelDsIntSlider.addOnSliderTouchListener(this)
        binding.levelDsDecimalSlider.addOnSliderTouchListener(this)

        binding.levelDsSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                binding.searchMotionLayout.transitionToEnd()
                binding.levelText.text =
                    context.getString(R.string.search_level_ds, searchLevelDs)
            } else {
                binding.searchMotionLayout.transitionToStart()
                binding.levelText.text =
                    context.getString(R.string.search_level_string, searchLevelString)
            }
        }

        binding.searchButton.setOnClickListener {
            search()
        }
        binding.searchResetBtn.setOnClickListener {
            binding.searchEditText.setText("")
            binding.levelDsSwitch.isChecked = false
            binding.levelSortSpinner.setSelection(0)
            binding.levelDsDecimalSlider.value = 0f
            binding.levelDsIntSlider.value = 1f
            binding.levelSlider.value = 0f
            genreCheckBoxList.forEach { it.isChecked = false }
            versionCheckBoxList.forEach { it.isChecked = false }
            binding.favorCheckbox.isChecked = false
            searchLevelString = levels[0]
            binding.levelText.text =
                context.getString(R.string.search_level_string, searchLevelString)
        }

        binding.searchEditText.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                search()
                true
            } else {
                false
            }
        }
    }

    fun setOnSearchResultListener(onSearchResultListener: OnSearchResultListener) {
        listener = onSearchResultListener
    }

    private fun search() {
        listener?.onResult(
            if (binding.levelDsSwitch.isChecked) {
                SongDataManager.search(
                    binding.searchEditText.text.toString(),
                    getSortCheck(),
                    getVersionCheck(),
                    null,
                    BigDecimal(searchLevelDs.toDouble()).setScale(1, RoundingMode.HALF_UP)
                        .toDouble(),
                    null, binding.favorCheckbox.isChecked
                )
            } else {
                SongDataManager.search(
                    binding.searchEditText.text.toString(),
                    getSortCheck(),
                    getVersionCheck(),
                    searchLevelString,
                    null,
                    binding.levelSortSpinner.selectedItem.toString(),
                    binding.favorCheckbox.isChecked
                )
            }
        )
        binding.searchEditText.clearFocus()
    }

    private fun getSortCheck(): List<String> {
        val sortList = mutableListOf<String>()
        for (cb in genreCheckBoxList) {
            if (cb.isChecked) sortList.add(cb.text.toString())
        }
        return sortList
    }

    private fun getVersionCheck(): List<String> {
        val versionList = mutableListOf<String>()
        for (cb in versionCheckBoxList) {
            if (cb.isChecked) versionList.add(cb.text.toString())
        }
        return versionList
    }

    interface OnSearchResultListener {
        fun onResult(list: List<SongData>)
    }

    override fun onStartTrackingTouch(slider: Slider) {

    }

    override fun onStopTrackingTouch(slider: Slider) {
        when (slider) {
            binding.levelSlider -> {
                val index = binding.levelSlider.value.toInt()
                searchLevelString = levels.getOrNull(index) ?: "UNKNOWN"
                binding.levelText.text =
                    context.getString(R.string.search_level_string, searchLevelString)
            }

            binding.levelDsIntSlider, binding.levelDsDecimalSlider -> {
                searchLevelDs =
                    binding.levelDsIntSlider.value + binding.levelDsDecimalSlider.value / 10
                binding.levelText.text =
                    context.getString(R.string.search_level_ds, searchLevelDs)
            }
        }
    }
}