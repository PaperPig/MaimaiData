package com.paperpig.maimaidata.ui.maimaidxprober

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.android.synthetic.main.activity_prober.*
import kotlinx.android.synthetic.main.mmd_splash_style_bg_layout.*
import kotlinx.android.synthetic.main.title.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProberActivity : AppCompatActivity() {
    private lateinit var proberVersionAdapter: ProberVersionAdapter


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prober)


        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = getString(R.string.maimaidx_prober)

        setupAnimation()

        CoroutineScope(Dispatchers.Main).launch {
            val data = getData()

            proberVp.apply {
                proberVersionAdapter = ProberVersionAdapter(data)
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
                .subscribe({
                    refreshLayout.isRefreshing = false
                    val hasStatus = it.asJsonObject.has("status")
                    if (hasStatus) {
                        if (it.asJsonObject.get("status").asString == "error") {
                            Toast.makeText(this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT)
                                .show()
                            startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
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

                    }
                }, { error ->
                    refreshLayout.isRefreshing = false
                    error.printStackTrace()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
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
}