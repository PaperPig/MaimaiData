package com.paperpig.maimaidata.ui.checklist

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import androidx.appcompat.app.AppCompatActivity
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityLevelCheckBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.utils.ConvertUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LevelCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLevelCheckBinding
    private var dataList = listOf<SongData>()
    private var recordList = listOf<Record>()

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

        val levelArrays = resources.getStringArray(R.array.dxp_song_level).toMutableList()
        //删除第一个ALL等级
        levelArrays.removeAt(0)
        val levelArraysAdapter = LevelArrayAdapter(
            this@LevelCheckActivity, R.layout.item_spinner_level, levelArrays
        )
        levelArraysAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)


        CoroutineScope(Dispatchers.Main).launch {
            recordList = RecordRepository().getRecord(this@LevelCheckActivity)
            dataList = SongDataRepository().getData(this@LevelCheckActivity)

            binding.levelSpn.apply {
                adapter = levelArraysAdapter
                setSelection(4)
                onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?, view: View?, position: Int, id: Long
                    ) {
                        val level = ConvertUtils.getLevel(
                            parent?.getItemAtPosition(
                                position
                            ) as String
                        )
                        val filter = dataList.filter {
                            it.level.contains(level)
                        }

                        (binding.levelCheckRecyclerView.adapter as LevelCheckAdapter).updateData(
                            filter.sortedByDescending { it.level.contains(level) }, level
                        )

                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
            }

            binding.levelCheckRecyclerView.apply {
                val level = ConvertUtils.getLevel(
                    binding.levelSpn.selectedItem as String
                )
                adapter = LevelCheckAdapter(context,
                    dataList.filter {
                        it.level.contains(level)
                    }.sortedByDescending { it.level.contains(level) },
                    recordList,
                    level
                )

                layoutManager = FlexboxLayoutManager(context).apply {
                    flexDirection = FlexDirection.ROW
                    justifyContent = JustifyContent.FLEX_START // 设置主轴上的对齐方式为起始位置
                }
            }

        }

        binding.switchBtn.setOnClickListener {
            (binding.levelCheckRecyclerView.adapter as LevelCheckAdapter).updateDisplay()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

}