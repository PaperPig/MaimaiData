package com.paperpig.maimaidata.ui.songlist

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongListBinding
import com.paperpig.maimaidata.databinding.MmdMainStyleBgLayoutBinding
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.WindowsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.hypot


class SongListFragment : BaseFragment<FragmentSongListBinding>() {
    private lateinit var binding: FragmentSongListBinding
    private lateinit var backgroundBinding: MmdMainStyleBgLayoutBinding
    private lateinit var songAdapter: SongListAdapter

    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val scrollRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                if (isVisible) {
                    backgroundBinding.loopBgRecyclerView.scrollBy(1, 0)
                    mHandler.postDelayed(this, 50)
                }
            }
        }
    }


    companion object {

        @JvmStatic
        fun newInstance() =
            SongListFragment()

        const val TAG = "SongListFragment"
    }


    override fun getViewBinding(container: ViewGroup?): FragmentSongListBinding {
        binding = FragmentSongListBinding.inflate(layoutInflater, container, false)
        backgroundBinding = MmdMainStyleBgLayoutBinding.bind(binding.root)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundBinding.loopBgRecyclerView.apply {
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_pattern)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.songListRecyclerView.apply {
            songAdapter = SongListAdapter()
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

        setupAnimation()

        binding.songSearchLayout.closeLayout.setOnClickListener {
            showOrHideSearchBar()
        }

        binding.songSearchLayout.searchBarLayout.searchButton.setOnClickListener {
            songAdapter.search(
                binding.songSearchLayout.searchBarLayout.searchText.text.toString(),
                getSortCheck(),
                getVersionCheck(),
                binding.songSearchLayout.levelSpinner.selectedItem.toString(),
                binding.songSearchLayout.sortSpinner.selectedItem.toString(),
                binding.songSearchLayout.favorCheck.isChecked
            )
            hideKeyboard(it)
            showOrHideSearchBar()
        }

        binding.songSearchLayout.searchBarLayout.searchText.setOnEditorActionListener { _, i, _ ->
            when (i) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    songAdapter.search(
                        binding.songSearchLayout.searchBarLayout.searchText.text.toString(),
                        getSortCheck(),
                        getVersionCheck(),
                        binding.songSearchLayout.levelSpinner.selectedItem.toString(),
                        binding.songSearchLayout.sortSpinner.selectedItem.toString(),
                        binding.songSearchLayout.favorCheck.isChecked
                    )
                    hideKeyboard(view)
                    showOrHideSearchBar()
                }
            }
            false
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
                    }
                }
                return true
            }

        })
        loadData()
    }

    fun loadData() {
        CoroutineScope(Dispatchers.Main).launch {
            songAdapter.setData(
                SongDataRepository().getData(context)
                    .sortedByDescending { it.id.toInt() })
        }

    }


    private fun getSortCheck(): List<String> {
        val checkBoxList =
            listOf<CheckBox>(
                binding.songSearchLayout.popCheck,
                binding.songSearchLayout.nicoCheck,
                binding.songSearchLayout.touhouCheck,
                binding.songSearchLayout.gameVarietyCheck,
                binding.songSearchLayout.maimaiSortCheck,
                binding.songSearchLayout.ongekiChuniCheck,
                binding.songSearchLayout.utageCheck
            )
        val sortList = mutableListOf<String>()
        for (cb in checkBoxList) {
            if (cb.isChecked) sortList.add(cb.text.toString())
        }
        return sortList
    }


    private fun getVersionCheck(): List<String> {
        val checkBoxList = listOf<CheckBox>(
            binding.songSearchLayout.maimaiCheck,
            binding.songSearchLayout.greenCheck,
            binding.songSearchLayout.orangeCheck,
            binding.songSearchLayout.pinkCheck,
            binding.songSearchLayout.murasakiCheck,
            binding.songSearchLayout.milkCheck,
            binding.songSearchLayout.finaleCheck,
            binding.songSearchLayout.dxCheck,
            binding.songSearchLayout.dx2021Check,
            binding.songSearchLayout.dx2022Check,
            binding.songSearchLayout.dx2023Check,
            binding.songSearchLayout.dx2024Check
        )
        val versionList = mutableListOf<String>()
        for (cb in checkBoxList) {
            if (cb.isChecked) versionList.add(cb.text.toString())
        }
        return versionList
    }


    private fun showOrHideSearchBar() {
        val windowWidth = WindowsUtils.getWindowWidth(context)
        val radius =
            hypot(
                binding.songSearchLayout.searchLayout.width.toDouble(),
                binding.songSearchLayout.searchLayout.height.toDouble()
            ).toFloat()

        if (binding.songSearchLayout.searchLayout.isVisible) {
            val createCircularReveal = ViewAnimationUtils.createCircularReveal(
                binding.songSearchLayout.searchLayout, windowWidth.toInt(), 0, radius, 0f
            )
            createCircularReveal.setDuration(300).doOnEnd {
                binding.songSearchLayout.searchLayout.visibility = View.GONE
            }
            createCircularReveal.start()

        } else {
            val createCircularReveal = ViewAnimationUtils.createCircularReveal(
                binding.songSearchLayout.searchLayout, windowWidth.toInt(), 0, 0f, radius
            )
            createCircularReveal.setDuration(300).doOnStart {
                binding.songSearchLayout.searchLayout.visibility = View.VISIBLE
            }
            createCircularReveal.start()
        }
    }

    private fun setupAnimation() {
        mHandler.postDelayed(scrollRunnable, 100)

        val animator =
            ObjectAnimator.ofFloat(backgroundBinding.mainBgSpeaker, "translationY", 0f, 20f, 0f)
                .apply {
                    duration = 500L
                    repeatCount = ValueAnimator.INFINITE
                }
        animator.start()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden){
            mHandler.postDelayed(scrollRunnable, 50)
        }
    }


    override fun onResume() {
        super.onResume()
        mHandler.postDelayed(scrollRunnable, 50)

    }

    override fun onPause() {
        super.onPause()
        mHandler.removeCallbacks(scrollRunnable)
    }
}