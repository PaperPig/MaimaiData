package com.paperpig.maimaidata.ui.maimaidxprober

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.model.SongListModel
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.ui.songlist.DotsScrollAdapter
import com.paperpig.maimaidata.utils.CreateBest40
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.android.synthetic.main.activity_prober.*
import kotlinx.android.synthetic.main.mmd_universe_style_bg_layout.*
import kotlinx.android.synthetic.main.title.*
import kotlinx.coroutines.*


class ProberActivity : AppCompatActivity() {
    private lateinit var proberVersionAdapter: ProberVersionAdapter
    private var songData = listOf<SongData>()
    private var oldRating = listOf<Record>()
    private var newRating = listOf<Record>()

    private val mHandler: Handler = Handler()
    private val scrollRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                dosTopRecyclerView.scrollBy(1, 0)
                dosUnderRecyclerView.scrollBy(1, 0)
                mHandler.postDelayed(this, 50)
            }
        }
    }

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
            songData = SongListModel().getData(this@ProberActivity)

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
                            records.sortedByDescending {
                                it.ra
                            }.filter {
                                val find = songData.find { data -> data.id == it.song_id }
                                !find!!.basic_info.is_new
                            }.let {
                                it.subList(0, if (it.size >= 25) 25 else it.size)
                            }
                        newRating =
                            records.sortedByDescending {
                                it.ra
                            }.filter {
                                val find = songData.find { data -> data.id == it.song_id }
                                find!!.basic_info.is_new
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
        val topLayoutManager = LinearLayoutManager(this)
        topLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        dosTopRecyclerView.apply {
            layoutManager = topLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_top)
        }
        val underLayoutManager = LinearLayoutManager(this)
        underLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        dosUnderRecyclerView.apply {
            layoutManager = underLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_under)
        }
        mHandler.postDelayed(scrollRunnable, 100)
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

    override fun onResume() {
        super.onResume()
        mHandler.postDelayed(scrollRunnable, 100)
    }

    override fun onStop() {
        super.onStop()
        mHandler.removeCallbacks(scrollRunnable)
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