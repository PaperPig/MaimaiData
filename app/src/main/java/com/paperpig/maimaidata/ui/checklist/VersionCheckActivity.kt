package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MediatorLiveData
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityVersionCheckBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.model.Version
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.utils.SharePreferencesUtils

class VersionCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVersionCheckBinding
    private var dataList = listOf<SongWithChartsEntity>()
    private var recordList = listOf<RecordEntity>()
    private lateinit var sharedPrefs: SharePreferencesUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVersionCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.version_query)
        sharedPrefs = SharePreferencesUtils(this)

        getData()
    }

    private fun getData() {
        //获取所有的歌曲
        val allSongs = SongWithChartRepository.getInstance(
            AppDataBase.getInstance().songWithChartDao(),
        ).getAllSongWithCharts()
        //获取MASTER难度记录
        val allRecords =
            RecordRepository.getInstance(AppDataBase.getInstance().recordDao())
                .getRecordsByDifficultyIndex(3)
        //使用MediatorLiveData来监听两个LiveData的变化
        MediatorLiveData<Pair<List<SongWithChartsEntity>, List<RecordEntity>>>().apply {
            addSource(allSongs) { songs ->
                val records = allRecords.value ?: emptyList()
                value = Pair(songs, records)
            }
            addSource(allRecords) { records ->
                val songs = allSongs.value ?: emptyList()
                value = Pair(songs, records)
            }
            observe(this@VersionCheckActivity) {
                if (it.first.isNotEmpty() && it.second.isNotEmpty()) {
                    dataList = it.first
                    recordList = it.second
                    initView()
                }
            }
        }
    }


    private fun initView() {
        val lastSelectedPosition = sharedPrefs.getLastQueryVersion()
        val versionList = getVersionList()

        //设置spinner适配器
        binding.versionSpn.apply {
            adapter = VersionArrayAdapter(
                this@VersionCheckActivity,
                R.layout.item_spinner_version,
                versionList
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(lastSelectedPosition, true)
            onItemSelectedListener =
                object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        sharedPrefs.saveLastQueryVersion(position)

                        (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateData(
                            dataList.filter {
                                (it.songData.from == (parent?.getItemAtPosition(
                                    position
                                ) as Version).versionName)
                            }.sortedByDescending { it.charts[3].ds })
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }

                }

        }

        //设置recyclerView适配器
        binding.versionCheckRecycler.apply {
            adapter =
                VersionCheckAdapter(
                    context,
                    dataList.filter {
                        it.songData.from == versionList[lastSelectedPosition].versionName
                    }.sortedByDescending { it.charts[3].ds }, recordList
                )

            layoutManager = FlexboxLayoutManager(context).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.FLEX_START // 设置主轴上的对齐方式为起始位置
            }
        }

        binding.switchBtn.setOnClickListener {
            (binding.versionCheckRecycler.adapter as VersionCheckAdapter).updateDisplay()
        }
    }

    private fun getVersionList(): MutableList<Version> {
        return mutableListOf(
            Version("maimai", R.drawable.maimai),
            Version("maimai PLUS", R.drawable.maimai_plus),
            Version("maimai GreeN", R.drawable.maimai_green),
            Version("maimai GreeN PLUS", R.drawable.maimai_green_plus),
            Version("maimai ORANGE", R.drawable.maimai_orange),
            Version("maimai ORANGE PLUS", R.drawable.maimai_orange),
            Version("maimai PiNK", R.drawable.maimai_pink),
            Version("maimai PiNK PLUS", R.drawable.maimai_pink_plus),
            Version("maimai MURASAKi", R.drawable.maimai_murasaki),
            Version("maimai MURASAKi PLUS", R.drawable.maimai_murasaki_plus),
            Version("maimai MiLK", R.drawable.maimai_milk),
            Version("maimai MiLK PLUS", R.drawable.maimai_milk_plus),
            Version("maimai FiNALE", R.drawable.maimai_finale),
            Version("舞萌DX", R.drawable.maimaidx_cn),
            Version("舞萌DX 2021", R.drawable.maimaidx_2021),
            Version("舞萌DX 2022", R.drawable.maimaidx_2022),
            Version("舞萌DX 2023", R.drawable.maimaidx_2023),
            Version("舞萌DX 2024", R.drawable.maimaidx_2024),
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }

}