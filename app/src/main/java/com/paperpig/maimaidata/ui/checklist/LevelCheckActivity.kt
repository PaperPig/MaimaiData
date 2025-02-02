package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.repository.RecordDataManager
import com.paperpig.maimaidata.repository.SongDataManager

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


        binding.levelCheckRecycler.apply {
            binding.levelSlider.apply {
                addOnChangeListener { _, value, _ ->
                    val index = value.toInt()
                    searchLevelString = levelArrays.getOrNull(index) ?: "UNKNOWN"
                    binding.levelText.text =
                        context.getString(R.string.search_level_string, searchLevelString)
                    refreshDataList()
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

        binding.levelSlider.value = 18f
        searchLevelString = levelArrays[binding.levelSlider.value.toInt()]



        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateDisplay()
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