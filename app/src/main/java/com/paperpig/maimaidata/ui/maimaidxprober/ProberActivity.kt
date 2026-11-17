package com.paperpig.maimaidata.ui.maimaidxprober

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.afollestad.materialdialogs.MaterialDialog
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityProberBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.network.lxns.LxnsClient
import com.paperpig.maimaidata.network.lxns.LxnsRecordConverter
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.ui.BaseActivity
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.CreateBest50
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.PermissionHelper
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ProberActivity : BaseActivity<ActivityProberBinding>() {

    companion object {
        const val EXTRA_PROBER_SOURCE = "extra_prober_source"
        const val SOURCE_DIVING_FISH = "diving_fish"
        const val SOURCE_LXNS = "lxns"

        fun actionStart(context: Context, source: String = SOURCE_DIVING_FISH) {
            val intent = Intent(context, ProberActivity::class.java).apply {
                putExtra(EXTRA_PROBER_SOURCE, source)
            }
            context.startActivity(intent)
        }
    }

    private var proberSource: String = SOURCE_DIVING_FISH
    private lateinit var proberVersionAdapter: ProberVersionAdapter
    private var recordList = listOf<RecordEntity>()
    private var oldRating = listOf<RecordEntity>()
    private var newRating = listOf<RecordEntity>()
    private var dataList = listOf<SongWithChartsEntity>()

    private lateinit var permissionHelper: PermissionHelper

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionHelper.onRequestPermissionsResult(result)
        }

    override fun inflateBinding() = ActivityProberBinding.inflate(layoutInflater)

    override fun topInsetTargetView(): View = binding.toolbarLayout.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        proberSource = intent.getStringExtra(EXTRA_PROBER_SOURCE) ?: SOURCE_DIVING_FISH

        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        supportActionBar?.title = if (proberSource == SOURCE_LXNS) {
            getString(R.string.lxns_prober)
        } else {
            getString(R.string.maimaidx_prober)
        }
        binding.oldVersionRdoBtn.text = String.format(getString(R.string.old_version_35), 0)
        binding.newVersionRdoBtn.text = String.format(getString(R.string.new_version_15), 0)

        permissionHelper = PermissionHelper.with(this)


        SongWithChartRepository.getInstance(AppDataBase.getInstance().songWithChartDao())
            .getAllSongWithCharts().observe(this) {
                dataList = it
                setData(dataList)
            }

    }

    private fun setData(list: List<SongWithChartsEntity>) {
        binding.proberVp.apply {
            proberVersionAdapter = ProberVersionAdapter(list)
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

        getRecord(list)

    }

    private fun getRecord(list: List<SongWithChartsEntity>) {
        if (proberSource == SOURCE_LXNS) {
            getLxnsRecord(list)
        } else {
            getDivingFishRecord(list)
        }
    }

    private fun getLxnsRecord(list: List<SongWithChartsEntity>) {
        val token = SpUtil.getLxnsUserToken()
        if (token.isBlank()) {
            binding.refreshLayout.isRefreshing = false
            Toast.makeText(this, R.string.vpn_please_bind_lxns, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                val (player, scores) = withContext(Dispatchers.IO) {
                    val playerInfo = runCatching {
                        LxnsClient.instance.fetchUserPlayer(token)
                    }.getOrNull()
                    val scoreList = LxnsClient.instance.fetchUserScores(token)
                    playerInfo to scoreList
                }
                player?.let {
                    SpUtil.saveLxnsAccount(token, it.friendCode, it.name, it.rating)
                }

                val convertRecord = LxnsRecordConverter.convert(scores, list)
                applyRecordsToUi(convertRecord, list)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@ProberActivity,
                    e.message ?: "落雪查分请求出错，请检查个人密钥与网络",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                binding.refreshLayout.isRefreshing = false
            }
        }
    }

    @SuppressLint("CheckResult")
    private fun getDivingFishRecord(list: List<SongWithChartsEntity>) {
        MaimaiDataRequests.getRecords(SpUtil.getCookie()).subscribe({ it ->
            binding.refreshLayout.isRefreshing = false
            val hasStatus = it.asJsonObject.has("status")
            if (hasStatus) {
                if (it.asJsonObject.get("status").asString == "error") {
                    Toast.makeText(
                        this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT
                    ).show()
                    startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
                    finish()
                    return@subscribe
                }
            }

            if (Settings.getEnableDivingFishNickname()) {
                val hasNickname = it.asJsonObject.has("nickname")
                if (hasNickname) {
                    SpUtil.saveDivingFishNickname(it.asJsonObject.get("nickname").asString)
                } else {
                    SpUtil.saveDivingFishNickname("")
                }
            }

            val hasRecords = it.asJsonObject.has("records")
            if (hasRecords) {
                val convertRecord = JsonConvertToDb.convertRecord(it.asJsonObject.get("records"))
                applyRecordsToUi(convertRecord, list)
            }
        }, { error ->
            binding.refreshLayout.isRefreshing = false
            error.printStackTrace()
            Toast.makeText(this@ProberActivity, "请求出错，请重新登录", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this@ProberActivity, LoginActivity::class.java))
            finish()
        })
    }

    private fun applyRecordsToUi(
        convertRecord: List<RecordEntity>,
        list: List<SongWithChartsEntity>
    ) {
        lifecycleScope.launch {
            RecordRepository.getInstance(AppDataBase.getInstance().recordDao())
                .replaceAllRecord(convertRecord)
        }

        recordList = convertRecord
        proberVersionAdapter.setData(recordList)

        if (!proberVersionAdapter.isDataMatching()) {
            MaterialDialog.Builder(this@ProberActivity)
                .title(getString(R.string.mismatching_data_title))
                .content(getString(R.string.mismatching_data_content))
                .positiveText(R.string.common_confirm).show()
        }

        oldRating = recordList.sortedByDescending {
            it.ra
        }.filter {
            val find = list.find { data -> data.songData.id == it.songId }
            find != null && find.songData.genre != Constants.GENRE_UTAGE && !find.songData.isNew
        }.let {
            it.subList(0, if (it.size >= 35) 35 else it.size)
        }
        newRating = recordList.sortedByDescending {
            it.ra
        }.filter {
            val find = list.find { data -> data.songData.id == it.songId }
            find != null && find.songData.genre != Constants.GENRE_UTAGE && find.songData.isNew
        }.let {
            it.subList(0, if (it.size >= 15) 15 else it.size)
        }

        binding.oldVersionRdoBtn.text = String.format(
            getString(R.string.old_version_35), oldRating.sumOf {
                ConvertUtils.achievementToRating(
                    Math.round(it.ds * 10).toInt(),
                    Math.round(it.achievements * 10000).toInt(),
                    it.isAp()
                )
            })
        binding.newVersionRdoBtn.text = String.format(
            getString(R.string.new_version_15), newRating.sumOf {
                ConvertUtils.achievementToRating(
                    Math.round(it.ds * 10).toInt(),
                    Math.round(it.achievements * 10000).toInt(),
                    it.isAp()
                )
            })
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.share_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.menu_share -> permissionHelper.registerLauncher(requestPermissionLauncher)
                .checkStoragePermission(object : PermissionHelper.PermissionCallback {
                    override fun onAllGranted() {
                        createImage()
                    }

                    override fun onDenied(deniedPermissions: List<String>) {
                        Toast.makeText(
                            this@ProberActivity,
                            getString(R.string.storage_permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
        }

        return true
    }


    private fun createImage() {
        lifecycleScope.launch {
            binding.loading.visibility = View.VISIBLE
            val preferredNickname = if (proberSource == SOURCE_LXNS) {
                SpUtil.getLxnsPlayerName().takeIf { it.isNotBlank() }
            } else {
                null
            }
            CreateBest50.createSongInfo(
                this@ProberActivity, dataList, oldRating, newRating, preferredNickname
            )

            binding.loading.visibility = View.GONE
        }

    }


    override fun onResume() {
        super.onResume()
        binding.maimaiBgView.resumeAnimation()
    }

    override fun onPause() {
        super.onPause()
        binding.maimaiBgView.pauseAnimation()
    }


}