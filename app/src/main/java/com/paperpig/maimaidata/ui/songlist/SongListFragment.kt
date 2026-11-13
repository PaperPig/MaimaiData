package com.paperpig.maimaidata.ui.songlist

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongListBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.repository.SongWithChartRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.MainActivity
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.widgets.SearchLayout
import com.paperpig.maimaidata.widgets.Settings
import me.zhanghai.android.fastscroll.FastScrollerBuilder


class SongListFragment : BaseFragment<FragmentSongListBinding>() {
    private lateinit var binding: FragmentSongListBinding
    private lateinit var songAdapter: SongListAdapter

    private val showAnimator by lazy {
        ValueAnimator.ofInt(binding.searchLayout.height, binding.root.height).apply {
            addUpdateListener { animation ->
                binding.searchLayout.layoutParams.height = animation.animatedValue as Int
                binding.searchLayout.requestLayout()
            }
            doOnStart {
                binding.searchLayout.visibility = View.VISIBLE
            }
        }
    }

    private val hiddenAnimator by lazy {
        ValueAnimator.ofInt(binding.searchLayout.height, 0).apply {
            addUpdateListener { animation ->
                binding.searchLayout.layoutParams.height = animation.animatedValue as Int
                binding.searchLayout.requestLayout()
            }
            doOnEnd { binding.searchLayout.visibility = View.INVISIBLE }
        }
    }

    private var isShowingSearchLayout = false

    private val searchBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            if (binding.searchLayout.isVisible) {
                showOrHideSearchBar()
                hideKeyboard(view)
            }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance() = SongListFragment()

        const val TAG = "SongListFragment"
    }


    override fun getViewBinding(container: ViewGroup?): FragmentSongListBinding {
        binding = FragmentSongListBinding.inflate(layoutInflater, container, false)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isShowingSearchLayout = binding.searchLayout.isVisible

        FastScrollerBuilder(binding.songListRecyclerView).build()
        binding.songListRecyclerView.apply {
            songAdapter = SongListAdapter()
            songAdapter.setCompactMode(SpUtil.isSongListCompact())
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
            itemAnimator = SongListItemAnimator()
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (isHidden || isShowingSearchLayout) return
                    (activity as? MainActivity)?.onContentScrolled(
                        dy = dy,
                        canScrollUp = recyclerView.canScrollVertically(-1)
                    )
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE &&
                        !isHidden &&
                        !isShowingSearchLayout &&
                        !recyclerView.canScrollVertically(-1)
                    ) {
                        (activity as? MainActivity)?.setBottomBarVisible(true)
                    }
                }
            })
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                val isHiddenFragment = isHidden
                menu.findItem(R.id.prioritize_new_songs)?.apply {
                    isVisible = !isHiddenFragment
                    if (isPrioritizeNewSongsEnabled()) {
                        setIcon(R.drawable.ic_new_songs_priority_on)
                        setTitle(R.string.prioritize_new_songs_on)
                    } else {
                        setIcon(R.drawable.ic_new_songs_priority_off)
                        setTitle(R.string.prioritize_new_songs_off)
                    }
                }
                menu.findItem(R.id.search)?.isVisible = !isHiddenFragment
                menu.findItem(R.id.switch_layout)?.apply {
                    isVisible = !isHiddenFragment
                    if (songAdapter.isCompactMode) {
                        setIcon(R.drawable.ic_view_agenda)
                        setTitle(R.string.switch_to_normal_layout)
                    } else {
                        setIcon(R.drawable.ic_view_compact)
                        setTitle(R.string.switch_to_compact_layout)
                    }
                }
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.prioritize_new_songs -> {
                        togglePrioritizeNewSongs()
                        return true
                    }

                    R.id.search -> {
                        showOrHideSearchBar()
                        hideKeyboard(view)
                        return true
                    }

                    R.id.switch_layout -> {
                        toggleLayoutMode()
                        return true
                    }
                }
                return false
            }

        }, viewLifecycleOwner)

        searchBackCallback.isEnabled = isShowingSearchLayout
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            searchBackCallback
        )
        (activity as? MainActivity)?.let {
            updateBottomPadding(it.getBottomContentPaddingPx(), it.getSystemBottomInsetPx())
        }
        loadData()
    }

    private var lastToggleTime = 0L

    private fun canToggleToolbarAction(): Boolean {
        val now = System.currentTimeMillis()
        // 智能防抖：若正在播放切换动画或 250ms 内快速连击，则忽略，避免动画踩踏
        if (now - lastToggleTime < 250L || binding.songListRecyclerView.itemAnimator?.isRunning == true) {
            return false
        }
        lastToggleTime = now
        // 停止当前滚动，锁定坐标基准，防止惯性滚动中切换导致视觉穿帮
        binding.songListRecyclerView.stopScroll()
        return true
    }

    private fun animateToolbarItem(itemId: Int) {
        binding.root.postOnAnimation {
            activity?.findViewById<View>(itemId)?.apply {
                animate().cancel()
                alpha = 0.35f
                animate()
                    .alpha(1f)
                    .setDuration(180L)
                    .start()
            }
        }
    }

    private fun fadeRefreshSongList(scrollToTop: Boolean) {
        val recyclerView = binding.songListRecyclerView
        recyclerView.animate().cancel()
        recyclerView.animate()
            .alpha(0f)
            .setDuration(120L)
            .withEndAction {
                if (!isAdded || !recyclerView.isAttachedToWindow) {
                    recyclerView.alpha = 1f
                    return@withEndAction
                }
                applySongList(scrollToTop = scrollToTop)
                recyclerView.animate()
                    .alpha(1f)
                    .setDuration(160L)
                    .start()
            }
            .start()
    }

    private fun toggleLayoutMode() {
        if (!canToggleToolbarAction()) return

        val newMode = !songAdapter.isCompactMode
        songAdapter.setCompactMode(newMode)
        SpUtil.setSongListCompact(newMode)
        requireActivity().invalidateOptionsMenu()
        animateToolbarItem(R.id.switch_layout)
    }


    private var isPrioritizeNewSongs: Boolean? = null
    private var baseSongList: List<com.paperpig.maimaidata.db.entity.SongWithChartsEntity> =
        emptyList()

    private fun isPrioritizeNewSongsEnabled(): Boolean {
        return isPrioritizeNewSongs
            ?: com.paperpig.maimaidata.utils.SpUtil.isPrioritizeNewSongs().also {
                isPrioritizeNewSongs = it
            }
    }

    private fun togglePrioritizeNewSongs() {
        if (!canToggleToolbarAction()) return

        val nextState = !isPrioritizeNewSongsEnabled()
        isPrioritizeNewSongs = nextState
        com.paperpig.maimaidata.utils.SpUtil.setPrioritizeNewSongs(nextState)
        fadeRefreshSongList(scrollToTop = true)
        (activity as? MainActivity)?.setBottomBarVisible(true)
        requireActivity().invalidateOptionsMenu()
        animateToolbarItem(R.id.prioritize_new_songs)
        android.widget.Toast.makeText(
            requireContext(),
            if (nextState) R.string.prioritize_new_songs_on else R.string.prioritize_new_songs_off,
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }

    private fun updateSongList(
        songs: List<com.paperpig.maimaidata.db.entity.SongWithChartsEntity>,
        scrollToTop: Boolean = false
    ) {
        baseSongList = songs
        applySongList(scrollToTop = scrollToTop)
    }

    private fun applySongList(scrollToTop: Boolean = false) {
        val displayList = if (isPrioritizeNewSongsEnabled()) {
            baseSongList.sortedByDescending { it.songData.isLatestUpdate }
        } else {
            baseSongList
        }
        songAdapter.setData(displayList)
        if (scrollToTop && displayList.isNotEmpty()) {
            binding.songListRecyclerView.scrollToPosition(0)
        }
    }

    fun loadData() {
        SongWithChartRepository.getInstance(AppDataBase.getInstance().songWithChartDao())
            .getAllSongWithCharts().observe(requireActivity()) {

                updateSongList(it.filterNot { it.songData.genre == Constants.GENRE_UTAGE })

                binding.searchLayout.setOnSearchResultListener(it, object :
                    SearchLayout.OnSearchListener {
                    override fun onSearch(
                        searchText: String,
                        genreList: List<String>,
                        versionList: List<String>,
                        selectLevel: String?,
                        sequencing: String?,
                        ds: Double?,
                        isFavor: Boolean
                    ) {
                        val repository = SongWithChartRepository.getInstance(
                            AppDataBase.getInstance().songWithChartDao()
                        )
                        repository.searchSongsWithCharts(
                            searchText,
                            genreList,
                            versionList,
                            selectLevel,
                            sequencing,
                            ds,
                            isFavor,
                            Settings.getEnableAliasSearch(),
                            Settings.getEnableCharterSearch(),
                            true
                        )
                            .observe(requireActivity()) {
                                updateSongList(it, scrollToTop = true)
                                showOrHideSearchBar()
                                hideKeyboard(view)
                            }
                    }
                })
            }
    }

    private fun showOrHideSearchBar() {
        binding.root.requestFocus()
        if (!isShowingSearchLayout) {
            hiddenAnimator.cancel()
            showAnimator.run {
                setIntValues(binding.searchLayout.height, binding.root.height)
                start()
            }
        } else {
            showAnimator.cancel()
            hiddenAnimator.run {
                setIntValues(binding.searchLayout.height, 0)
                start()
            }
        }
        isShowingSearchLayout = !isShowingSearchLayout
        searchBackCallback.isEnabled = !isHidden && isShowingSearchLayout
        (activity as? MainActivity)?.setBottomBarVisible(!isShowingSearchLayout)
    }

    fun updateBottomPadding(contentPaddingBottom: Int, systemBottomInset: Int) {
        if (!isAdded) return
        binding.songListRecyclerView.updatePadding(bottom = contentPaddingBottom)
        binding.searchLayout.updateBottomInset(systemBottomInset)
    }

    /**
     * 再次点击底栏当前 Tab：优先收起搜索面板，否则平滑滚动回列表顶部
     */
    fun onTabReselected() {
        if (!isAdded) return
        if (binding.searchLayout.isVisible) {
            showOrHideSearchBar()
            hideKeyboard(view)
            return
        }
        val layoutManager = binding.songListRecyclerView.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        if (firstVisible > 18) {
            binding.songListRecyclerView.scrollToPosition(18)
        }
        binding.songListRecyclerView.smoothScrollToPosition(0)
        (activity as? MainActivity)?.setBottomBarVisible(true)
    }


    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        searchBackCallback.isEnabled = !hidden && isShowingSearchLayout
        requireActivity().invalidateOptionsMenu()
        if (hidden) {
            binding.maimaiBgView.pauseAnimation()
        } else {
            binding.maimaiBgView.resumeAnimation()
            (activity as? MainActivity)?.setBottomBarVisible(!isShowingSearchLayout)
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            binding.maimaiBgView.resumeAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.maimaiBgView.pauseAnimation()
    }

}
