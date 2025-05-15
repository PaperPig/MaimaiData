package com.paperpig.maimaidata.ui.rating

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.crawler.CrawlerCaller
import com.paperpig.maimaidata.crawler.WechatCrawlerListener
import com.paperpig.maimaidata.databinding.FragmentRatingBinding
import com.paperpig.maimaidata.model.Rating
import com.paperpig.maimaidata.network.server.HttpServerService
import com.paperpig.maimaidata.network.vpn.core.LocalVpnService
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.about.SettingsActivity
import com.paperpig.maimaidata.ui.checklist.LevelCheckActivity
import com.paperpig.maimaidata.ui.checklist.VersionCheckActivity
import com.paperpig.maimaidata.ui.finaletodx.FinaleToDxActivity
import com.paperpig.maimaidata.ui.maimaidxprober.LoginActivity
import com.paperpig.maimaidata.ui.maimaidxprober.ProberActivity
import com.paperpig.maimaidata.utils.ConvertUtils
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.getInt
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.floor
import kotlin.random.Random

class RatingFragment : BaseFragment<FragmentRatingBinding>(), WechatCrawlerListener,
    LocalVpnService.onStatusChangedListener {

    private lateinit var binding: FragmentRatingBinding

    private lateinit var resultAdapter: RatingResultAdapter

    private val proberUpdateDialog by lazy { ProberUpdateDialog(requireContext()) }


    private val httpServiceIntent by lazy {
        Intent(requireContext(), HttpServerService::class.java)
    }

    private val vpnActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startProxyServices()
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            RatingFragment()

        const val TAG = "RatingFragment"

    }

    override fun getViewBinding(container: ViewGroup?): FragmentRatingBinding {
        binding = FragmentRatingBinding.inflate(layoutInflater, container, false)
        return binding
    }

    override fun onResume() {
        super.onResume()
        if (SpUtil.getUserName().isEmpty()) {
            binding.accountText.setText(R.string.no_logged_in)
            binding.proberQueryBtn.visibility = View.GONE
            binding.proberLoginBtn.setText(R.string.login)
        } else {
            binding.accountText.text = SpUtil.getUserName()
            binding.proberQueryBtn.visibility = View.VISIBLE
            binding.proberLoginBtn.setText(R.string.change_account)

        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ratingResultRecycler.apply {
            resultAdapter = RatingResultAdapter()
            layoutManager = LinearLayoutManager(context)
            adapter = resultAdapter
        }

        binding.proberLoginBtn.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.proberQueryBtn.setOnClickListener {
            startActivity(Intent(context, ProberActivity::class.java))
        }

        binding.proberLoginBtn.setOnClickListener {
            startActivity(Intent(context, LoginActivity::class.java))
        }

        binding.proberLevelCheckBtn.setOnClickListener {
            startActivity(Intent(context, LevelCheckActivity::class.java))
        }

        binding.proberVersionCheckBtn.setOnClickListener {
            startActivity(Intent(context, VersionCheckActivity::class.java))
        }

        binding.proberFinaleToDxBtn.setOnClickListener {
            startActivity(Intent(context, FinaleToDxActivity::class.java))
        }

        binding.calculateBtn.setOnClickListener {
            hideKeyboard(view)
            onCalculate(binding.targetRatingEdit.text.toString())
        }

        binding.inputSongLevel.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本变化前
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时
            }

            override fun afterTextChanged(s: Editable?) {
                // 文本变化后
                val songLevel = s.toString()
                val songAchievementText = binding.inputSongAchievement.text.toString()
                if (songAchievementText.isNotEmpty() && songAchievementText != "."
                    && songLevel.isNotEmpty() && songLevel != "."){
                    binding.outputSingleRating.text = ConvertUtils.achievementToRating(
                        (binding.inputSongLevel.text.toString().toFloat() * 10).toInt(),
                        (binding.inputSongAchievement.text.toString().toFloat() * 10000).toInt()
                    ).toString()
                } else {
                    binding.outputSingleRating.text = "0"
                }
            }
        })

        binding.inputSongAchievement.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本变化前
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本变化时
            }

            override fun afterTextChanged(s: Editable?) {
                // 文本变化后
                val songAchievement = s.toString()
                val songLevelText = binding.inputSongLevel.text.toString()
                if (songAchievement.isNotEmpty() && songAchievement != "."
                    && songLevelText.isNotEmpty() && songLevelText != "."){
                    binding.outputSingleRating.text = ConvertUtils.achievementToRating(
                        (binding.inputSongLevel.text.toString().toFloat() * 10).toInt(),
                        (binding.inputSongAchievement.text.toString().toFloat() * 10000).toInt()
                    ).toString()
                } else {
                    binding.outputSingleRating.text = "0"
                }
            }
        })

        CrawlerCaller.setOnWechatCrawlerListener(this)
        LocalVpnService.addOnStatusChangedListener(this)

        binding.proberProxySimpleText.setOnClickListener {
            proberUpdateDialog.show()
        }

        binding.proberProxyUpdateBtn.setOnClickListener {
            if (!LocalVpnService.IsRunning) {
                val intent: Intent? = LocalVpnService.prepare(context)
                if (intent == null) {
                    startProxyServices()
                } else {
                    vpnActivityResultLauncher.launch(intent)
                }
            } else {
                LocalVpnService.IsRunning = false
                stopHttpService()
            }
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.settings).isVisible = !isHidden
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.about_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.settings -> {
                        startActivity(Intent(requireContext(), SettingsActivity::class.java))
                        return true
                    }
                }
                return false
            }

        })
    }

    private fun startProxyServices() {
        if (SpUtil.getUserName().isEmpty()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.vpn_please_login),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startVPNService()
        startHttpService()
        createLinkUrl()
        getWechatApi()
    }


    private fun createLinkUrl() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val randomChar = (1..10)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")

        val link = "http://127.0.0.2:8284/$randomChar"

        val mClipData = ClipData.newPlainText("copyText", link)
        (requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
            mClipData
        )

        Toast.makeText(
            requireContext(),
            "已复制链接，请在微信中粘贴并打开",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun getWechatApi() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            val cmp = ComponentName("com.tencent.mm", "com.tencent.mm.ui.LauncherUI")
            intent.apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                component = cmp
            }
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    private fun startVPNService() {
        requireContext().startService(Intent(requireContext(), LocalVpnService::class.java))
    }

    private fun startHttpService() {
        requireContext().startService(httpServiceIntent)
    }

    private fun stopHttpService() {
        requireContext().stopService(httpServiceIntent)
    }

    private fun showToast() {
        val text = "请输入内容!"
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(this.context, text, duration)
        toast.show()
    }

    private fun onCalculate(targetString: String) {
        val targetRating = targetString.getInt()
        if (targetRating <= 0) {
            showToast()
            return
        }
        val rating = targetRating / 50

        val minLv = getReachableLevel(rating)
        val map = mutableMapOf<Int, Int>()
        val list = mutableListOf<Rating>()

        for (i in 150 downTo minLv) {

            when (val reachableAchievement = getReachableAchievement(i, rating)) {
                800000, 900000, 940000 ->
                    map[reachableAchievement] = i

                in 970000..1010000 ->
                    map[reachableAchievement] = i
            }
        }

        map.forEach {
            list.add(
                Rating(
                    (it.value / 10f),
                    String.format(Locale.getDefault(), "%.4f%%", it.key / 10000f),
                    ConvertUtils.achievementToRating(it.value, it.key),
                    ConvertUtils.achievementToRating(it.value, it.key) * 50
                )
            )
        }

        resultAdapter.setData(list)

    }

    override fun onStatusChanged(status: String, isRunning: Boolean) {
        binding.proberProxyUpdateBtn.setText(if (isRunning) R.string.stop_proxy else R.string.start_proxy)
    }

    @SuppressLint("SetTextI18n")
    override fun onLogReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
    }

    @SuppressLint("SetTextI18n")
    override fun onMessageReceived(logString: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $logString\n")
        binding.proberProxySimpleText.text = "[$timestamp] $logString"
    }

    override fun onStartAuth() {
        binding.proberProxySimpleText.text = ""
        binding.proberProxyIndicator.isIndeterminate = true
        binding.proberProxyStatusGroup.visibility = View.VISIBLE
    }

    override fun onFinishUpdate() {
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
    }

    @SuppressLint("SetTextI18n")
    override fun onError(e: Exception) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis())
        proberUpdateDialog.appendText("[$timestamp] $e\n")
        binding.proberProxySimpleText.text = "[$timestamp] $e"
        binding.proberProxyIndicator.visibility = View.INVISIBLE
        stopHttpService()
    }

    override fun onDestroy() {
        super.onDestroy()
        CrawlerCaller.removeOnWechatCrawlerListener()
        LocalVpnService.removeOnStatusChangedListener(this)
    }
}


private fun getReachableLevel(rating: Int): Int {
    for (i in 10..150) {
        if (rating < ConvertUtils.achievementToRating(i, 1005000)) {
            return i
        }
    }
    return 0
}

private fun getReachableAchievement(level: Int, rating: Int): Int {
    var maxAchi = 1010000
    var minAchi = 0
    var tempAchi: Int


    if (ConvertUtils.achievementToRating(level, 1005000) < rating)
        return 1010001
    for (n in 0..20) {
        if (maxAchi - minAchi >= 2) {
            tempAchi = floor((maxAchi.toDouble() + minAchi) / 2).toInt()

            if (ConvertUtils.achievementToRating(level, tempAchi) < rating)
                minAchi = tempAchi
            else maxAchi = tempAchi
        }
    }
    return maxAchi
}
