package com.paperpig.maimaidata.ui.nameplate

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityNamePlateBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.model.SongListModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NamePlateActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNamePlateBinding
    private var dataList = listOf<SongData>()
    private var recordList = ArrayList<Record>()
    private val fragmentList = ArrayList<Fragment>()

    companion object {
        fun actionStart(context: Context, record: ArrayList<Record>) {
            val intent = Intent(context, NamePlateActivity::class.java).apply {
                putParcelableArrayListExtra("record", record)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNamePlateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.name_plate_query)




        recordList = intent.getParcelableArrayListExtra("record") ?: arrayListOf()
        CoroutineScope(Dispatchers.Main).launch {
            dataList = SongListModel().getData(this@NamePlateActivity) as ArrayList<SongData>

            val filter =
                dataList.filter { (it.basic_info.from == "maimai" || it.basic_info.from == "maimai PLUS") && it.basic_info.title != "ジングルベル" }
            fragmentList.add(NamePlateFragment.newInstance(ArrayList(filter), recordList, 0))
            fragmentList.add(NamePlateFragment.newInstance(ArrayList(filter), recordList, 1))
            fragmentList.add(NamePlateFragment.newInstance(ArrayList(filter), recordList, 2))
            fragmentList.add(NamePlateFragment.newInstance(ArrayList(filter), recordList, 3))

            binding.viewPager.adapter =
                NamePlateFragmentAdapter(supportFragmentManager, -1, fragmentList)
            binding.tabLayout.setupWithViewPager(binding.viewPager)

            binding.versionSpn.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                        val filterList = when (p2) {
                            0 -> {
                                dataList.filter { (it.basic_info.from == "maimai" || it.basic_info.from == "maimai PLUS") && it.basic_info.title != "ジングルベル" }
                            }
                            12 -> {
                                dataList.filter { it.basic_info.from.contains("でらっくす") && !it.basic_info.is_new }
                            }
                            else -> {
                                dataList.filter { it.basic_info.from == p0?.getItemAtPosition(p2) }
                            }
                        }


                        fragmentList.clear()
                        fragmentList.add(
                            NamePlateFragment.newInstance(
                                ArrayList(filterList),
                                recordList,
                                0
                            )
                        )
                        fragmentList.add(
                            NamePlateFragment.newInstance(
                                ArrayList(filterList),
                                recordList,
                                1
                            )
                        )
                        fragmentList.add(
                            NamePlateFragment.newInstance(
                                ArrayList(filterList),
                                recordList,
                                2
                            )
                        )
                        fragmentList.add(
                            NamePlateFragment.newInstance(
                                ArrayList(filterList),
                                recordList,
                                3
                            )
                        )

                        binding.viewPager.adapter =
                            NamePlateFragmentAdapter(supportFragmentManager, -1, fragmentList)

                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {

                    }

                }
        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }

    inner class NamePlateFragmentAdapter(
        fragmentManager: FragmentManager,
        behavior: Int,
        private val list: List<Fragment>
    ) : FragmentStatePagerAdapter(fragmentManager, behavior) {
        private val titleArray = arrayOf("MAS", "EXP", "ADV", "BAS")

        override fun getPageTitle(position: Int): CharSequence {
            return titleArray[position]
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getItem(position: Int): Fragment {
            return list[position]
        }
    }
}