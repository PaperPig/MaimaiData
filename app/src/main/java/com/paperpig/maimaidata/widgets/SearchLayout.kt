package com.paperpig.maimaidata.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.flexbox.FlexLine
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.material.slider.Slider
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ItemSearchHistoryBinding
import com.paperpig.maimaidata.databinding.LayoutSongSearchBinding
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
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

    val transition: Transition by lazy {
        AutoTransition().apply {
            duration = 150
        }
    }

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
            binding.searchLayout.apply {
                TransitionManager.beginDelayedTransition(binding.searchLayout, transition)
                if (checked) {
                    binding.levelText.text =
                        context.getString(R.string.search_level_ds, searchLevelDs)

                    binding.dsGroup.visibility = VISIBLE
                    binding.levelGroup.visibility = GONE
                } else {
                    binding.levelText.text =
                        context.getString(R.string.search_level_string, searchLevelString)

                    binding.dsGroup.visibility = GONE
                    binding.levelGroup.visibility = VISIBLE
                }
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

        binding.searchHistoryDeleteIv.setOnClickListener {
            clearHistoryRecyclerView()
        }

        binding.searchHistoryRecyclerView.apply {
            val searchHistory = SpUtil.getSearchHistory()
            setSearchHistoryGroupVisible(searchHistory.isNotEmpty())
            adapter = HistoryAdapter(
                searchHistory
            ) { song ->
                binding.searchEditText.setText(song)
                search()
            }
            layoutManager = object : FlexboxLayoutManager(context) {

                val fixMaxLine = 2

                //超出2行的内容不显示
                override fun getFlexLinesInternal(): MutableList<FlexLine> {
                    val originList = super.getFlexLinesInternal()
                    val size = originList.size
                    if (size > fixMaxLine) {
                        originList.subList(fixMaxLine, size).clear()
                    }
                    return originList
                }
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
        updateHistoryRecyclerView(binding.searchEditText.text.toString())
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
    private fun updateHistoryRecyclerView(searchText: String) {
        if (searchText.isNotEmpty()) {
            SpUtil.saveSearchHistory(searchText)
            val updatedHistory = SpUtil.getSearchHistory()
            postDelayed({
                setSearchHistoryGroupVisible(true)
                (binding.searchHistoryRecyclerView.adapter as? HistoryAdapter)?.updateData(
                    updatedHistory
                )
            }, 500)
        }
    }

    private fun clearHistoryRecyclerView() {
        SpUtil.clearSearchHistory()
        setSearchHistoryGroupVisible(false)
        (binding.searchHistoryRecyclerView.adapter as? HistoryAdapter)?.updateData(emptyList())
    }


    private fun setSearchHistoryGroupVisible(isShow: Boolean) {
        binding.searchLayout.apply {
            if (isShow == (binding.searchHistoryGroup.isVisible)) {
                return
            }
            TransitionManager.beginDelayedTransition(binding.searchLayout, transition)
            binding.searchHistoryGroup.visibility = if (isShow) VISIBLE else GONE
        }
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

    class HistoryAdapter(
        private var historyList: List<String>,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        inner class HistoryViewHolder(binding: ItemSearchHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val textView = binding.historyItemText
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val binding = ItemSearchHistoryBinding.inflate(LayoutInflater.from(parent.context))
            return HistoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = historyList[position]
            holder.textView.text = item
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = historyList.size

        fun updateData(newData: List<String>) {
            historyList = newData
            notifyDataSetChanged()
        }
    }
}