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
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongListBinding
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.repository.SongDataManager
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.widgets.AnimationHelper
import com.paperpig.maimaidata.widgets.SearchLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.FastScrollerBuilder


class SongListFragment : BaseFragment<FragmentSongListBinding>() {
    private lateinit var binding: FragmentSongListBinding
    private lateinit var songAdapter: SongListAdapter
    private lateinit var animationHelper: AnimationHelper

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

    companion object {

        @JvmStatic
        fun newInstance() =
            SongListFragment()

        const val TAG = "SongListFragment"
    }


    override fun getViewBinding(container: ViewGroup?): FragmentSongListBinding {
        binding = FragmentSongListBinding.inflate(layoutInflater, container, false)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        animationHelper = AnimationHelper(layoutInflater)
        binding.root.addView(animationHelper.loadLayout(), 0)
        animationHelper.startAnimation()
        isShowingSearchLayout = binding.searchLayout.visibility == View.VISIBLE

        binding.searchLayout.setOnSearchResultListener(object :
            SearchLayout.OnSearchResultListener {
            override fun onResult(list: List<SongData>) {
                songAdapter.setData(list)
                showOrHideSearchBar()
                hideKeyboard(view)
            }
        })

        FastScrollerBuilder(binding.songListRecyclerView).build()
        binding.songListRecyclerView.apply {
            songAdapter = SongListAdapter()
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.search).isVisible = !isHidden
            }

            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.search -> {
                        showOrHideSearchBar()
                        hideKeyboard(view)
                        return true
                    }
                }
                return false
            }

        })

        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.searchLayout.visibility == View.VISIBLE) {
                    showOrHideSearchBar()
                } else {
                    requireActivity().finish()
                }
            }
        }
        )
        loadData()
    }


    fun loadData() {
        CoroutineScope(Dispatchers.Main).launch {
            SongDataManager.loadData()
            //默认不显示宴会场数据
            songAdapter.setData(SongDataManager.list.filterNot { it.basic_info.genre == Constants.GENRE_UTAGE })
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