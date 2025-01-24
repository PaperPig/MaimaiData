package com.paperpig.maimaidata.ui.songlist

import android.os.Bundle
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
import com.paperpig.maimaidata.repository.SongDataRepository
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.ui.animation.AnimationHelper
import com.paperpig.maimaidata.utils.WindowsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.hypot


class SongListFragment : BaseFragment<FragmentSongListBinding>() {
    private lateinit var binding: FragmentSongListBinding
    private lateinit var songAdapter: SongListAdapter
    private lateinit var animationHelper:AnimationHelper

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
        binding.root.addView(animationHelper.loadLayout(),0)
        animationHelper.startAnimation()

        binding.songListRecyclerView.apply {
            songAdapter = SongListAdapter()
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

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