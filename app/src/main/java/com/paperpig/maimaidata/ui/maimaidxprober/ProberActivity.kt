package com.paperpig.maimaidata.ui.maimaidxprober

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import kotlinx.android.synthetic.main.activity_prober.*
import kotlinx.android.synthetic.main.title.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProberActivity : AppCompatActivity() {
    private lateinit var recordAdapter: RecordAdapter


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

        CoroutineScope(Dispatchers.Main).launch {
            val data = getData()
            proberRecyclerView.apply {
                recordAdapter = RecordAdapter(data)
                adapter = recordAdapter
                layoutManager = LinearLayoutManager(this@ProberActivity)
            }



            MaimaiDataRequests.getRecords(SharePreferencesUtils(this@ProberActivity).getCookie())
                .subscribe({
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
                        recordAdapter.setData(records)
                    }
                }, { error ->
                    error.printStackTrace()
                })
        }
        versionGrp.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.oldVersionRdoBtn -> recordAdapter.setVersion(0)
                R.id.newVersionRdoBtn -> recordAdapter.setVersion(1)

            }
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
}