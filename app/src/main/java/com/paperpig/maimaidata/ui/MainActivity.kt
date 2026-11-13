package com.paperpig.maimaidata.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentTransaction
import androidx.interpolator.view.animation.FastOutLinearInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityMainBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.repository.ChartRepository
import com.paperpig.maimaidata.repository.ChartStatsRepository
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import com.paperpig.maimaidata.utils.JsonConvertToDb
import com.paperpig.maimaidata.utils.SpUtil
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.launch

class MainActivity : BaseActivity<ActivityMainBinding>() {

    private lateinit var ratingFragment: RatingFragment
    private lateinit var songListFragment: SongListFragment
    private var updateDisposable: Disposable? = null
    private var checkChartStatusDisposable: Disposable? = null
    private var isUpdateChecked = false
    private var downloadTask: DownloadTask? = null
    private var currentTabId: Int = R.id.navDXSongList
    private var isBottomBarVisible = true
    private var isImeVisible = false
    private var systemBottomInsetPx = 0
    private var accumulatedScrollDy = 0
    private val scrollThresholdPx by lazy {
        (12f * resources.displayMetrics.density).toInt()
    }

    private val tabBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (currentTabId != R.id.navDXSongList) {
                binding.mainBottomNaviView.selectedItemId = R.id.navDXSongList
            }
        }
    }

    override fun inflateBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun topInsetTargetView(): View = binding.toolbarLayout.toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolbarLayout.toolbar)

        checkChartStatus()

        queryMaxNotes()

        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt("CURRENT_TAB_ID", R.id.navDXSongList)
            supportActionBar?.title = savedInstanceState.getString("TOOLBAR_TITLE")

            supportFragmentManager.getFragment(
                savedInstanceState, SongListFragment.TAG
            )?.apply {
                songListFragment = this as SongListFragment
            }

            supportFragmentManager.getFragment(
                savedInstanceState, RatingFragment.TAG
            )?.apply {
                ratingFragment = this as RatingFragment
            }
        } else {
            showFragment(R.id.navDXSongList, animate = false)
        }

        tabBackCallback.isEnabled = currentTabId != R.id.navDXSongList
        onBackPressedDispatcher.addCallback(this, tabBackCallback)

        setupBottomNavigation()
    }

    override fun onApplyCustomEdgeToEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainContentContainer) { _, insets ->
            val navBars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeNowVisible = ime.bottom > 0

            if (systemBottomInsetPx != navBars.bottom) {
                systemBottomInsetPx = navBars.bottom
                dispatchBottomPaddingToFragments()
            }

            if (isImeVisible != imeNowVisible || imeNowVisible) {
                isImeVisible = imeNowVisible
                binding.fragmentContent.updatePadding(bottom = ime.bottom)
                binding.bottomNavContainer.visibility =
                    if (imeNowVisible) View.GONE else View.VISIBLE
                if (!imeNowVisible) {
                    binding.bottomNavContainer.translationY =
                        if (isBottomBarVisible) 0f else binding.bottomNavContainer.height.toFloat()
                }
            }
            insets
        }

        binding.bottomNavContainer.addOnLayoutChangeListener { _, _, top, _, bottom, _, oldTop, _, oldBottom ->
            if ((bottom - top) != (oldBottom - oldTop)) {
                dispatchBottomPaddingToFragments()
            }
        }
    }

    fun getSystemBottomInsetPx(): Int = systemBottomInsetPx

    fun getBottomContentPaddingPx(): Int {
        val extraGap = (8f * resources.displayMetrics.density).toInt()
        val navHeight = binding.bottomNavContainer.height
        return if (navHeight > 0) {
            navHeight + extraGap
        } else {
            (56f * resources.displayMetrics.density).toInt() + systemBottomInsetPx
        }
    }

    private fun dispatchBottomPaddingToFragments() {
        val contentPadding = getBottomContentPaddingPx()
        if (::songListFragment.isInitialized && songListFragment.isAdded) {
            songListFragment.updateBottomPadding(contentPadding, systemBottomInsetPx)
        }
        if (::ratingFragment.isInitialized && ratingFragment.isAdded) {
            ratingFragment.updateBottomPadding(contentPadding)
        }
    }

    private fun setupBottomNavigation() {
        val navView = binding.mainBottomNaviView
        // 覆盖 Material3 BottomNavigationMenuView 默认 80dp 最小高度，保持紧凑纯图标栏
        navView.getChildAt(0)?.minimumHeight =
            (48f * resources.displayMetrics.density).toInt()

        // 禁用长按默认 Tooltip，避免弹出冗余黑色文字气泡
        navView.menu.forEach { menuItem ->
            navView.findViewById<View>(menuItem.itemId)?.let { itemView ->
                TooltipCompat.setTooltipText(itemView, null)
            }
        }

        navView.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId) {
                return@setOnItemSelectedListener true
            }
            navView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            hideCurrentKeyboard()
            animateNavIcon(item.itemId)
            showFragment(item.itemId, animate = true)
            true
        }

        navView.setOnItemReselectedListener { item ->
            navView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            animateNavIcon(item.itemId)
            when (item.itemId) {
                R.id.navDXSongList -> {
                    if (::songListFragment.isInitialized) {
                        songListFragment.onTabReselected()
                    }
                }

                R.id.navDxTarget -> {
                    if (::ratingFragment.isInitialized) {
                        ratingFragment.onTabReselected()
                    }
                }
            }
        }
    }

    private fun animateNavIcon(itemId: Int) {
        val itemView = binding.mainBottomNaviView.findViewById<View>(itemId) ?: return
        val iconView =
            itemView.findViewById<View>(com.google.android.material.R.id.navigation_bar_item_icon_view)
                ?: itemView
        iconView.animate().cancel()
        iconView.scaleX = 0.86f
        iconView.scaleY = 0.86f
        iconView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(220L)
            .setInterpolator(OvershootInterpolator(2.0f))
            .start()
    }

    private fun hideCurrentKeyboard() {
        val focusView = currentFocus ?: binding.root
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(focusView.windowToken, 0)
        currentFocus?.clearFocus()
    }

    /**
     * 根据页面垂直滚动增量自动隐藏/显示底栏
     * @param dy 本次垂直滚动像素（> 0 表示页面内容向上走/用户向下浏览）
     * @param canScrollUp 内容是否还可以继续向上滚动（false 表示已触顶）
     */
    fun onContentScrolled(dy: Int, canScrollUp: Boolean) {
        if (isImeVisible) return
        if (!canScrollUp) {
            accumulatedScrollDy = 0
            setBottomBarVisible(true)
            return
        }
        if (dy == 0) return

        // 方向切换时清零累计值，防止来回晃动抖动
        if ((dy > 0 && accumulatedScrollDy < 0) || (dy < 0 && accumulatedScrollDy > 0)) {
            accumulatedScrollDy = 0
        }
        accumulatedScrollDy += dy

        if (accumulatedScrollDy > scrollThresholdPx && isBottomBarVisible) {
            accumulatedScrollDy = 0
            setBottomBarVisible(false)
        } else if (accumulatedScrollDy < -scrollThresholdPx && !isBottomBarVisible) {
            accumulatedScrollDy = 0
            setBottomBarVisible(true)
        }
    }

    fun setBottomBarVisible(visible: Boolean) {
        if (isBottomBarVisible == visible) return
        isBottomBarVisible = visible
        accumulatedScrollDy = 0

        val container = binding.bottomNavContainer
        container.animate().cancel()
        if (visible) {
            container.animate()
                .translationY(0f)
                .setDuration(220L)
                .setInterpolator(LinearOutSlowInInterpolator())
                .start()
        } else {
            val hideAction = {
                val targetY = container.height.toFloat()
                if (targetY > 0f) {
                    container.animate()
                        .translationY(targetY)
                        .setDuration(180L)
                        .setInterpolator(FastOutLinearInInterpolator())
                        .start()
                }
            }
            if (container.height > 0) {
                hideAction()
            } else {
                container.post(hideAction)
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("CURRENT_TAB_ID", currentTabId)
        outState.putString("TOOLBAR_TITLE", supportActionBar?.title.toString())
        if (::songListFragment.isInitialized) supportFragmentManager.putFragment(
            outState, SongListFragment.TAG, songListFragment
        )
        if (::ratingFragment.isInitialized) supportFragmentManager.putFragment(
            outState, RatingFragment.TAG, ratingFragment
        )
    }

    override fun onResume() {
        super.onResume()
        if (!isUpdateChecked) {
            updateDisposable?.dispose()
            checkUpdate()
        }
    }

    /**
     * check update
     */
    private fun checkUpdate() {
        updateDisposable = MaimaiDataRequests.fetchUpdateInfo().subscribe({
            isUpdateChecked = true
            if (it.version != null && it.version!! > BuildConfig.VERSION_NAME && !it.url.isNullOrBlank()) {
                MaterialDialog.Builder(this).title(
                    this@MainActivity.getString(
                        R.string.maimai_data_update_title, it.version
                    )
                ).content(
                    (it.info
                        ?: this@MainActivity.getString(R.string.maimai_data_update_default_content)).replace(
                        "\\n", "\n"
                    )
                ).positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel).onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(it.url)
                            })
                        }
                    }.onNegative { d, _ ->
                        d.dismiss()
                    }.autoDismiss(true).cancelable(true).show()
            } else {
                val nextDataVersion = it.dataVersion5
                val nextDataUrl = it.dataUrl5
                if (!nextDataVersion.isNullOrBlank()
                    && !nextDataUrl.isNullOrBlank()
                    && SpUtil.getDataVersion() < nextDataVersion
                ) {
                    MaterialDialog.Builder(this)
                        .title(this@MainActivity.getString(R.string.maimai_data_data_update_title))
                        .content(
                            String.format(
                                this@MainActivity.getString(R.string.maimai_data_data_update_info),
                                SpUtil.getDataVersion(),
                                nextDataVersion
                            )
                        ).positiveText(R.string.maimai_data_update_download)
                        .negativeText(R.string.common_cancel).onPositive { _, which ->
                            if (DialogAction.POSITIVE == which) {
                                startDataDownload(it)
                            }
                        }.onNegative { d, _ ->
                            d.dismiss()
                        }.autoDismiss(true).cancelable(true).show()
                }
            }
        }, {
            it.printStackTrace()
        })
    }

    /**
     * 查询最大notes数量
     */
    private fun queryMaxNotes() {
        ChartRepository.getInstance(AppDataBase.getInstance().chartDao()).getMaxNotes()
            .observe(this) {
                MaimaiDataApplication.instance.maxNotesStats = it
            }
    }

    /**
     * 检查水鱼谱面数据
     */
    private fun checkChartStatus() {
        //每五天更新数据
        val lastUpdateTime = SpUtil.getLastUpdateChartStats()
        val currentTime = System.currentTimeMillis()
        val fiveDaysMillis = 5 * 24 * 60 * 60 * 1000L
        if ((currentTime - lastUpdateTime) >= fiveDaysMillis) {
            checkChartStatusDisposable = MaimaiDataRequests.getChartStatus().subscribe(
                { t ->
                    lifecycleScope.launch {
                        val convertChatStats = JsonConvertToDb.convertChatStats(t)
                        val result = ChartStatsRepository.getInstance(
                            AppDataBase.getInstance().chartStatsDao()
                        ).replaceAllChartStats(convertChatStats)
                        if (result) {
                            SpUtil.saveLastUpdateChartStats(currentTime)
                        }
                    }
                }, {
                    it.printStackTrace()
                    Toast.makeText(this, "谱面状态数据下载失败", Toast.LENGTH_LONG).show()
                })
        }
    }

    private fun startDataDownload(appUpdateModel: AppUpdateModel) {
        val dataUrl = appUpdateModel.dataUrl5?.takeIf { it.isNotBlank() } ?: return
        val dataVersion = appUpdateModel.dataVersion5?.takeIf { it.isNotBlank() } ?: return
        val updateDialog =
            MaterialDialog.Builder(this).title(getString(R.string.maimai_data_download_title))
                .content(getString(R.string.maimai_data_start_download)).cancelable(false).show()
        downloadTask =
            DownloadTask.Builder(
                dataUrl,
                filesDir.path,
                "songdata.json"
            )
                .setMinIntervalMillisCallbackProcess(16).setPassIfAlreadyCompleted(false).build()


        downloadTask!!.enqueue(object : DownloadListener3() {
            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }

            override fun connected(
                task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
            ) {
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                updateDialog.setContent("$currentOffset/$totalLength")
            }

            override fun started(task: DownloadTask) {
                updateDialog.show()
            }

            override fun completed(task: DownloadTask) {
                updateDialog.dismiss()
                lifecycleScope.launch {
                    val data = SongDataRepository().getData(this@MainActivity)
                    val result = SongWithChartRepository.getInstance(
                        AppDataBase.getInstance().songWithChartDao()
                    ).updateDatabase(data)
                    if (result) {
                        SpUtil.setDataVersion(dataVersion)
                    }
                }
            }

            override fun canceled(task: DownloadTask) {
                updateDialog.dismiss()
            }

            override fun error(task: DownloadTask, e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.maimai_data_download_error),
                    Toast.LENGTH_SHORT
                ).show()
                updateDialog.dismiss()
            }

            override fun warn(task: DownloadTask) {
            }

        })
    }

    private fun showFragment(tabId: Int, animate: Boolean = true) {
        currentTabId = tabId
        tabBackCallback.isEnabled = tabId != R.id.navDXSongList
        setBottomBarVisible(true)
        invalidateMenu()
        val ft = supportFragmentManager.beginTransaction()
        if (animate) {
            ft.setCustomAnimations(R.anim.nav_fade_in, R.anim.nav_fade_out)
        }
        hideAllFragment(ft)
        when (tabId) {

            R.id.navDxTarget -> {
                supportActionBar?.setTitle(R.string.dx_rating_correlation)
                if (!::ratingFragment.isInitialized) {
                    ratingFragment = RatingFragment.newInstance()
                    ft.add(R.id.fragment_content, ratingFragment, RatingFragment.TAG)
                } else {
                    ft.show(ratingFragment)
                }
            }

            R.id.navDXSongList -> {
                supportActionBar?.setTitle(R.string.dx_song_list)
                if (!::songListFragment.isInitialized) {
                    songListFragment = SongListFragment.newInstance()
                    ft.add(R.id.fragment_content, songListFragment, SongListFragment.TAG)
                } else {
                    ft.show(songListFragment)
                }

            }
        }
        ft.commit()
    }

    private fun hideAllFragment(ft: FragmentTransaction) {
        ft.apply {
            if (::ratingFragment.isInitialized) {
                hide(ratingFragment)
            }
            if (::songListFragment.isInitialized) {
                hide(songListFragment)
            }
        }
    }
}