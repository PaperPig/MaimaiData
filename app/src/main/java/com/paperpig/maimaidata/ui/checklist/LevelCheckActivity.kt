package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.utils.SpUtil

class LevelCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLevelCheckBinding
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

        getData()
        initView()
    }

    private fun getData() {
        var songs: List<SongWithChartsEntity>? = null
        var records: List<RecordEntity>? = null
        //获取所有的歌曲
        val allSongs = SongWithChartRepository.getInstance(
            AppDataBase.getInstance().songWithChartDao(),
        ).getAllSongWithCharts()
        //获取所有的记录
        val allRecords =
            RecordRepository.getInstance(AppDataBase.getInstance().recordDao()).getAllRecord()
        //使用MediatorLiveData来监听两个LiveData的变化
        MediatorLiveData<Pair<List<SongWithChartsEntity>, List<RecordEntity>>>().apply {
            addSource(allSongs) { newSongs ->
                songs = newSongs
                if (songs != null && records != null) {
                    value = Pair(songs!!, records!!)
                }
            }
            addSource(allRecords) { newRecords ->
                records = newRecords
                if (songs != null && records != null) {
                    value = Pair(songs!!, records!!)
                }
            }
            observe(this@LevelCheckActivity) { (songs, records) ->
                (binding.levelCheckRecycler.adapter as LevelCheckAdapter).apply {
                    setData(songs, records)
                    updateData(searchLevelString)
                }
            }
        }
    }

    private fun initView() {
        //删除ALL等级标记
        val levelArrays =
            resources.getStringArray(R.array.dxp_song_level).toMutableList()
                .apply { removeAt(0) }
        binding.levelText.text = getString(R.string.search_level_string, levelArrays[0])

        //设置slide监听器
        binding.levelSlider.apply {
            value = 0f
            addOnChangeListener { _, value, _ ->
                val index = value.toInt()
                searchLevelString = levelArrays.getOrNull(index) ?: "UNKNOWN"
                binding.levelText.text =
                    context.getString(R.string.search_level_string, searchLevelString)
                (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateData(
                    searchLevelString
                )
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

        //设置RecyclerView适配器
        binding.levelCheckRecycler.apply {
            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START // 设置主轴上的对齐方式为起始位置
            }
            adapter = LevelCheckAdapter(context)
        }

        binding.levelSlider.value = SpUtil.getLastQueryLevel()
        searchLevelString = levelArrays[binding.levelSlider.value.toInt()]

        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecycler.adapter as LevelCheckAdapter).updateDisplay()
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

}