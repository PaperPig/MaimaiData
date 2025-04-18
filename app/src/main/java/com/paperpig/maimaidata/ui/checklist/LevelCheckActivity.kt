package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.repository.RecordDataManager
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.utils.SharePreferencesUtils

class LevelCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLevelCheckBinding
    private var dataList = listOf<SongWithChartsEntity>()
    private var recordList = listOf<Record>()
    private lateinit var sharedPrefs: SharePreferencesUtils
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
        sharedPrefs = SharePreferencesUtils(this)

        SongWithChartRepository.getInstance(AppDataBase.getInstance().songWithChartDao())
            .getAllSongAndCharts(false)
            .observe(this) {
                dataList = it
                initView()
            }
    }

    private fun initView() {
        //删除ALL等级标记
        val levelArrays =
            resources.getStringArray(R.array.dxp_song_level).toMutableList()
                .apply { removeAt(0) }

        recordList = RecordDataManager.list

        binding.levelCheckRecycler.apply {
            binding.levelSlider.apply {
                addOnChangeListener { _, value, _ ->
                    val index = value.toInt()
                    searchLevelString = levelArrays.getOrNull(index) ?: "UNKNOWN"
                    binding.levelText.text =
                        context.getString(R.string.search_level_string, searchLevelString)
                    refreshDataList()
                    sharedPrefs.saveLastQueryLevel(binding.levelSlider.value)
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

        binding.levelSlider.value = sharedPrefs.getLastQueryLevel()
        searchLevelString = levelArrays[binding.levelSlider.value.toInt()]

        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateDisplay()
        }
    }

    private fun refreshDataList() {
        binding.levelCheckRecycler.apply {
            if (adapter == null) {
                adapter = LevelCheckAdapter(
                    context,
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