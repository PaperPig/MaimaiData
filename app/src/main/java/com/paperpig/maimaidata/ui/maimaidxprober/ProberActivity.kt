package com.paperpig.maimaidata.ui.maimaidxprober

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityProberBinding
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.widgets.AnimationHelper
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.CreateBest50
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProberActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProberBinding

    private lateinit var proberVersionAdapter: ProberVersionAdapter
    private var songData = listOf<SongData>()
    private var recordData = arrayListOf<Record>()
    private var oldRating = listOf<Record>()
    private var newRating = listOf<Record>()

    private lateinit var animationHelper: AnimationHelper


    companion object {
        const val PERMISSION_REQUEST = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animationHelper = AnimationHelper(layoutInflater)
        binding.proberContainerLayout.addView(animationHelper.loadLayout(), 0)
        animationHelper.startAnimation()

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.maimaidx_prober)
        binding.oldVersionRdoBtn.text =
            String.format(getString(R.string.old_version_35), 0)
        binding.newVersionRdoBtn.text =
            String.format(getString(R.string.new_version_15), 0)


        CoroutineScope(Dispatchers.Main).launch {
            songData = SongDataRepository().getData(this@ProberActivity)
            if (songData.isEmpty()) return@launch

            binding.proberVp.apply {
                proberVersionAdapter = ProberVersionAdapter(songData)
                adapter = proberVersionAdapter
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (position == 0) {
                            binding.oldVersionRdoBtn.isChecked = true
                            binding.oldVersionIndicator.visibility = View.VISIBLE
                            binding.newVersionIndicator.visibility = View.GONE
                        } else {
                            binding.newVersionRdoBtn.isChecked = true
                            binding.oldVersionIndicator.visibility = View.GONE
                            binding.newVersionIndicator.visibility = View.VISIBLE
                        }
                    }
                })
            }


            binding.versionGroup.setOnCheckedChangeListener { _, i ->
                when (i) {
                    R.id.old_version_rdo_btn -> {
                        binding.proberVp.currentItem = 0
                        binding.oldVersionIndicator.visibility = View.VISIBLE
                        binding.newVersionIndicator.visibility = View.GONE
                    }

                    R.id.new_version_rdo_btn -> {
                        binding.proberVp.currentItem = 1
                        binding.oldVersionIndicator.visibility = View.GONE
                        binding.newVersionIndicator.visibility = View.VISIBLE
                    }
                }
            }


            binding.refreshLayout.apply {
                isEnabled = false
                isRefreshing = true
                setColorSchemeResources(R.color.colorPrimary)
            }

            MaimaiDataRequests.getRecords(SharePreferencesUtils(this@ProberActivity).getCookie())
                .subscribe({ it ->
                    binding.refreshLayout.isRefreshing = false
                    val hasStatus = it.asJsonObject.has("status")
                    if (hasStatus) {
                        if (it.asJsonObject.get("status").asString == "error") {
                            Toast.makeText(
                                this@ProberActivity,
                                "请求出错，请重新登录",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
                            finish()
                            return@subscribe
                        }
                    }

                    val hasRecords = it.asJsonObject.has("records")
                    if (hasRecords) {
                        CoroutineScope(Dispatchers.IO).launch {
                            RecordRepository().saveRecord(
                                this@ProberActivity,
                                it.asJsonObject.get("records")
                            )
                        }

                        val type = object : TypeToken<ArrayList<Record>>() {}.type
                        recordData = Gson().fromJson(
                            it.asJsonObject.get("records").asJsonArray, type
                        )
                        proberVersionAdapter.setData(recordData)

                        if (!proberVersionAdapter.isDataMatching()) {
                            MaterialDialog.Builder(this@ProberActivity)
                                .title(getString(R.string.mismatching_data_title))
                                .content(getString(R.string.mismatching_data_content))
                                .positiveText(R.string.common_confirm).show()
                        }

                        oldRating = recordData.sortedByDescending {
                            it.ra
                        }.filter {
                            val find = songData.find { data -> data.id == it.song_id }
                            if (find == null) false else !find.basic_info.is_new
                        }.let {
                            it.subList(0, if (it.size >= 35) 35 else it.size)
                        }
                        newRating =
                            recordData.sortedByDescending {
                                it.ra
                            }.filter {
                                val find = songData.find { data -> data.id == it.song_id }
                                find?.basic_info?.is_new ?: false
                            }.let {
                                it.subList(0, if (it.size >= 15) 15 else it.size)
                            }

                        binding.oldVersionRdoBtn.text =
                            String.format(getString(R.string.old_version_35),
                                oldRating.sumOf {
                                    ConvertUtils.achievementToRating(
                                        (it.ds * 10).toInt(),
                                        (it.achievements * 10000).toInt()
                                    )
                                }
                            )
                        binding.newVersionRdoBtn.text =
                            String.format(getString(R.string.new_version_15),
                                newRating.sumOf {
                                    ConvertUtils.achievementToRating(
                                        (it.ds * 10).toInt(),
                                        (it.achievements * 10000).toInt()
                                    )
                                }
                            )

                    }
                }, { error ->
                    binding.refreshLayout.isRefreshing = false
                    error.printStackTrace()
                    Toast.makeText(this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
                    finish()
                })
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()

            R.id.menu_share ->
                checkPermission()
//            R.id.menu_nameplate ->
//                NamePlateActivity.actionStart(this,recordData)
        }
        return true
    }


    private fun createImage() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.loading.visibility = View.VISIBLE
            CreateBest50.createSongInfo(
                this@ProberActivity,
                songData,
                oldRating,
                newRating
            )

            binding.loading.visibility = View.GONE
        }

    }


    private fun checkPermission() {
        val permissionsStorage = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, permissionsStorage, PERMISSION_REQUEST)
            } else {
                createImage()
            }
        } else {
            createImage()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createImage()
            } else {
                Toast.makeText(this, "保存失败，没有获取储存权限", Toast.LENGTH_SHORT).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    override fun onResume() {
        super.onResume()
        animationHelper.resumeAnimation()
    }

    override fun onPause() {
        super.onPause()
        animationHelper.pauseAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        animationHelper.stopAnimation()
    }


}