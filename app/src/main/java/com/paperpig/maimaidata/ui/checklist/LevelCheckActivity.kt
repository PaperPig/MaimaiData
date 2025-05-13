package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.repository.RecordDataManager
import com.paperpig.maimaidata.repository.SongDataManager
import com.paperpig.maimaidata.utils.SpUtil

class LevelCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLevelCheckBinding
    private var dataList = listOf<SongData>()
    private var recordList = listOf<Record>()
    private var searchLevelString = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLevelCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.level_query)
        //删除ALL等级标记
        val levelArrays =
            resources.getStringArray(R.array.dxp_song_level).toMutableList().apply { removeAt(0) }

        recordList = RecordDataManager.list
        dataList = SongDataManager.list

        fun refreshText(index: Int){
            searchLevelString = levelArrays.getOrNull(index) ?: "UNKNOWN"
            if (binding.levelSlider.value.toInt() == levelArrays.size - 1){
                binding.btnRight.isVisible = false
            } else if (binding.levelSlider.value.toInt() == 0){
                binding.btnLeft.isVisible = false
            } else {
                binding.btnRight.isVisible = true
                binding.btnLeft.isVisible = true
            }
        }

        binding.levelCheckRecycler.apply {
            binding.levelSlider.apply {
                addOnChangeListener { _, value, _ ->
                    val index = value.toInt()
                    refreshText(index)
                    binding.levelText.text =
                        context.getString(R.string.search_level_string, searchLevelString)
                    refreshDataList()
                    SpUtil.saveLastQueryLevel(binding.levelSlider.value)
                }
                setLabelFormatter { value ->
                    val index = value.toInt()
                    getString(
                        R.string.search_level_string,
                        levelArrays.getOrNull(index) ?: "UNKNOWN"
                    )
                }
            }
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START // 设置主轴上的对齐方式为起始位置
            }
        }

        binding.levelSlider.value = SpUtil.getLastQueryLevel()
        refreshText(binding.levelSlider.value.toInt())

        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateDisplay()
        }

        binding.btnLeft.setOnClickListener {
            if (binding.levelSlider.value.toInt() == 0){
                // ignored
            } else {
                binding.levelSlider.value -= 1f
            }
            refreshText(binding.levelSlider.value.toInt())
        }

        binding.btnRight.setOnClickListener {
            if (binding.levelSlider.value.toInt() == levelArrays.size - 1){
                // ignored
            } else {
                binding.levelSlider.value += 1f
            }
            refreshText(binding.levelSlider.value.toInt())
        }
    }

    private fun refreshDataList() {

        binding.levelCheckRecycler.apply {
            if (adapter == null) {
                adapter = LevelCheckAdapter(context,
                    dataList,
                    recordList,
                    searchLevelString
                )
            } else {
                (adapter as LevelCheckAdapter).updateData(searchLevelString)
            }
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

}