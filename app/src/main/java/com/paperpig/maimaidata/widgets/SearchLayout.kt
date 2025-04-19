package com.paperpig.maimaidata.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.google.android.material.slider.Slider
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.LayoutSongSearchBinding
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.versionCheck
import java.math.BigDecimal
import java.math.RoundingMode


class SearchLayout(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs),
    Slider.OnSliderTouchListener {
    private var listener: OnSearchListener? = null
    private val binding: LayoutSongSearchBinding =
        LayoutSongSearchBinding.inflate(LayoutInflater.from(context), this, true)

    private val levels = resources.getStringArray(R.array.dxp_song_level)
    private var dataList: List<SongWithChartsEntity> = listOf()

    private var searchLevelDs = 1.0f
    private var searchLevelString = ""

    private val spUtils = SharePreferencesUtils(
        MaimaiDataApplication.instance,
        SharePreferencesUtils.PREF_NAME_SONG_INFO
    )


    private val remasterComparator = Comparator<SongWithChartsEntity> { a, b ->
        when {
            a.charts.size < 5 && b.charts.size < 5 -> 0
            a.charts.size < 5 -> 1
            b.charts.size < 5 -> -1
            else -> 0
        }
    }

    private val remasterAscComparator = remasterComparator.thenBy { it.charts.getOrNull(4)?.ds }
    private val remasterDescComparator =
        remasterComparator.thenByDescending { it.charts.getOrNull(4)?.ds }

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

    fun setOnSearchResultListener(
        list: List<SongWithChartsEntity>,
        onSearchResultListener: OnSearchListener
    ) {
        dataList = list
        listener = onSearchResultListener
    }

    private fun search() {
        listener?.onSearch(
            binding.searchEditText.text.toString(),
            getSortCheck(),
            getVersionCheck(),
            if (binding.levelDsSwitch.isChecked) null else searchLevelString,
            if (binding.levelDsSwitch.isChecked) null else binding.levelSortSpinner.selectedItem.toString(),
            if (binding.levelDsSwitch.isChecked) BigDecimal(searchLevelDs.toDouble())
                .setScale(1, RoundingMode.HALF_UP).toDouble() else null,
            binding.favorCheckbox.isChecked
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

    interface OnSearchListener {
        fun onSearch(
            searchText: String,
            genreList: List<String>,
            versionList: List<String>,
            selectLevel: String?,
            sequencing: String?,
            ds: Double?,
            isFavor: Boolean
        )
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

    fun search(
        searchText: String = "",
        genreSortList: List<String> = emptyList(),
        versionSortList: List<String> = emptyList(),
        selectLevel: String? = null,
        levelDs: Double? = null,
        sequencing: String? = null,
        isShowFavor: Boolean = false
    ): List<SongWithChartsEntity> {
        return dataList.filter { it ->
            val song = it.songData
            val chart = it.charts
            // 歌曲名匹配
            val matchesSearch = when {
                searchText.isEmpty() || song.title.isEmpty() -> true
                else -> song.title.contains(searchText, true)
            }

            //todo 别称匹配
//            val matchesAlias = when {
//                !Settings.getEnableAliasSearch() -> false
//                searchText.isEmpty() -> true
//                song.alias == null -> false
//                else -> song.alias!!.any { it.contains(searchText, true) }
//            }

            // 流派匹配，默认不显示宴会场
            val matchesGenre = when {
                genreSortList.isNotEmpty() -> song.genre in genreSortList
                else -> song.genre != Constants.GENRE_UTAGE
            }

            // 版本匹配
            val matchesVersion = when {
                versionSortList.isNotEmpty() -> versionSortList.versionCheck(song.from)
                else -> true
            }


            // 等级匹配
            val matchesLevel = selectLevel?.let { level ->
                when {
                    level == "ALL" -> true
                    // 根据sequencing确定检查的等级位置
                    sequencing?.startsWith("EXPERT") == true -> chart.getOrNull(2)?.level == level
                    sequencing?.startsWith("MASTER") == true -> chart.getOrNull(3)?.level == level
                    sequencing?.startsWith("RE:MASTER") == true -> chart.getOrNull(4)?.level == level
                    else -> chart.any { it.level.contains(level) }
                }
            } != false

            // 定数匹配
            val matchesDs = levelDs?.let { ds -> chart.any { it.ds == ds } } != false

            // 是否收藏
            val matchesFavorite = !isShowFavor || spUtils.isFavorite(song.id.toString())

            (matchesSearch) && matchesGenre && matchesVersion && matchesLevel && matchesDs && matchesFavorite
        }.let { filteredList ->
            when (sequencing) {
                "EXPERT-升序" -> filteredList.sortedBy { it.charts.getOrNull(2)?.ds }
                "EXPERT-降序" -> filteredList.sortedByDescending { it.charts.getOrNull(2)?.ds }
                "MASTER-升序" -> filteredList.sortedBy { it.charts.getOrNull(3)?.ds }
                "MASTER-降序" -> filteredList.sortedByDescending { it.charts.getOrNull(3)?.ds }
                "RE:MASTER-升序" -> filteredList.sortedWith(remasterAscComparator)
                "RE:MASTER-降序" -> filteredList.sortedWith(remasterDescComparator)
                else -> filteredList.toList()
            }
        }


    }
}