package com.paperpig.maimaidata.ui.maimaidxprober

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.CreateBest40
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.android.synthetic.main.activity_prober.*
import kotlinx.android.synthetic.main.mmd_splash_style_bg_layout.*
import kotlinx.android.synthetic.main.title.*
import kotlinx.coroutines.*


class ProberActivity : AppCompatActivity() {


    private lateinit var proberVersionAdapter: ProberVersionAdapter
    private var songData = listOf<SongData>()
    private var oldRating = listOf<Record>()
    private var newRating = listOf<Record>()

    companion object {
        const val PERMISSION_REQUEST = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prober)


        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.maimaidx_prober)
        oldVersionRdoBtn.text =
            String.format(getString(R.string.old_version_25), 0)
        newVersionRdoBtn.text =
            String.format(getString(R.string.new_version_15), 0)

        setupAnimation()


        CoroutineScope(Dispatchers.Main).launch {
            songData = getData()

            proberVp.apply {
                proberVersionAdapter = ProberVersionAdapter(songData)
                adapter = proberVersionAdapter
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        if (position == 0) {
                            oldVersionRdoBtn.isChecked = true
                            oldVersionIndicator.visibility = View.VISIBLE
                            newVersionIndicator.visibility = View.GONE
                        } else {
                            newVersionRdoBtn.isChecked = true
                            oldVersionIndicator.visibility = View.GONE
                            newVersionIndicator.visibility = View.VISIBLE
                        }
                    }
                })
            }


            versionGroup.setOnCheckedChangeListener { _, i ->
                when (i) {
                    R.id.oldVersionRdoBtn -> {
                        proberVp.currentItem = 0
                        oldVersionIndicator.visibility = View.VISIBLE
                        newVersionIndicator.visibility = View.GONE
                    }
                    R.id.newVersionRdoBtn -> {
                        proberVp.currentItem = 1
                        oldVersionIndicator.visibility = View.GONE
                        newVersionIndicator.visibility = View.VISIBLE
                    }
                }
            }


            refreshLayout.apply {
                isEnabled = false
                isRefreshing = true
                setColorSchemeResources(R.color.colorPrimary)
            }

            MaimaiDataRequests.getRecords(SharePreferencesUtils(this@ProberActivity).getCookie())
                .subscribe({ it ->
                    refreshLayout.isRefreshing = false
                    val hasStatus = it.asJsonObject.has("status")
                    if (hasStatus) {
                        if (it.asJsonObject.get("status").asString == "error") {
                            Toast.makeText(this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
                            finish()
                            return@subscribe
                        }
                    }

                    val hasRecords = it.asJsonObject.has("records")
                    if (hasRecords) {
                        val type = object : TypeToken<List<Record>>() {}.type
                        val records =
                            Gson().fromJson<List<Record>>(it.asJsonObject.get("records").asJsonArray,
                                type)
                        proberVersionAdapter.setData(records)

                        oldRating =
                            records.filter { !it.is_new }.sortedByDescending {
                                it.ra
                            }.let {
                                it.subList(0, if (it.size >= 25) 25 else it.size)

                            }
                        newRating =
                            records.filter { it.is_new }.sortedByDescending {
                                it.ra
                            }.let {
                                it.subList(0, if (it.size >= 15) 15 else it.size)
                            }

                        oldVersionRdoBtn.text =
                            String.format(getString(R.string.old_version_25),
                                oldRating.sumBy { it.ra })
                        newVersionRdoBtn.text =
                            String.format(getString(R.string.new_version_15),
                                newRating.sumBy { it.ra })

                    }
                }, { error ->
                    refreshLayout.isRefreshing = false
                    error.printStackTrace()
                    Toast.makeText(this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT)
                        .show()
                    startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
                    finish()
                })
        }
    }

    private suspend fun getData(): List<SongData> {
        return withContext(Dispatchers.IO) {
            val song2021 = assets.open("music_data.json").bufferedReader()
                .use { it.readText() }
            Gson().fromJson<List<SongData>>(
                song2021, object : TypeToken<List<SongData>>() {}.type
            )
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
        }
        return true
    }


    private fun setupAnimation() {
        val translationAnimatorSet = AnimatorSet()
        translationAnimatorSet.playTogether(
            ObjectAnimator.ofFloat(bgGreen, "translationY", -20f, 20f, -20f).apply {
                duration = 10000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgYellow, "translationY", -20f, 20f, -20f).apply {
                duration = 120000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgBlue, "translationY", -20f, 20f, -20f).apply {
                duration = 8000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgOrange, "translationY", -20f, 20f, -20f).apply {
                duration = 7000L
                repeatCount = ValueAnimator.INFINITE
            }
        )

        translationAnimatorSet.start()
    }

    private fun createImage() {

        GlobalScope.launch(Dispatchers.Main) {

            loading.visibility = View.VISIBLE

            CreateBest40.createSongInfo(this@ProberActivity,
                songData,
                oldRating,
                newRating
            )

            loading.visibility = View.GONE


        }

    }


    private fun checkPermission() {
        val permissionsStorage = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
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
}