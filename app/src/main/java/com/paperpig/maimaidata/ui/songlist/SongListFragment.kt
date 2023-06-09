package com.paperpig.maimaidata.ui.songlist

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.FragmentSongListBinding
import com.paperpig.maimaidata.databinding.MmdUniverseStyleBgLayoutBinding
import com.paperpig.maimaidata.model.SongListModel
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.WindowsUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.hypot


class SongListFragment : BaseFragment<FragmentSongListBinding>() {
    private lateinit var binding: FragmentSongListBinding
    private lateinit var backgroundBinding: MmdUniverseStyleBgLayoutBinding
    private lateinit var songAdapter: SongListAdapter
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private val scrollRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                backgroundBinding.dosTopRecyclerView.scrollBy(1, 0)
                backgroundBinding.dosUnderRecyclerView.scrollBy(1, 0)
                mHandler.postDelayed(this, 50)
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
        backgroundBinding = MmdUniverseStyleBgLayoutBinding.bind(binding.root)
        return binding
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimation()

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
            songAdapter.setData(SongListModel().getData(context)
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
                binding.songSearchLayout.ongekiChuniCheck
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
            binding.songSearchLayout.dx2023Check
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
        val topLayoutManager = LinearLayoutManager(context)
        topLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        backgroundBinding.dosTopRecyclerView.apply {
            layoutManager = topLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_top)
        }
        val underLayoutManager = LinearLayoutManager(context)
        underLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        backgroundBinding.dosUnderRecyclerView.apply {
            layoutManager = underLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_under)
        }
        mHandler.postDelayed(scrollRunnable, 100)

        val animatorElement = arrayOf(
            backgroundBinding.baloonLeftB,
            backgroundBinding.baloonLeftDP,
            backgroundBinding.baloonLeftO,
            backgroundBinding.baloonLeftP,
            backgroundBinding.baloonLeftW1,
            backgroundBinding.baloonLeftW2,
            backgroundBinding.baloonLeftY,
            backgroundBinding.baloonRightB,
            backgroundBinding.baloonRightW2,
            backgroundBinding.baloonRightY,
            backgroundBinding.swirlH1,
            backgroundBinding.swirlO1,
            backgroundBinding.swirlO2,
            backgroundBinding.swirlO3,
            backgroundBinding.swirlO4,
            backgroundBinding.swirlB1
        )
        val translationAnimatorSet = AnimatorSet()

        for (elem in animatorElement) {
            val animator = ObjectAnimator.ofFloat(elem, "translationY", -20f, 20f, -20f).apply {
                duration = (8000L..16000L).random()
                repeatCount = ValueAnimator.INFINITE
            }
            translationAnimatorSet.playTogether(animator)
        }

        translationAnimatorSet.start()
    }

    override fun onResume() {
        super.onResume()
        mHandler.postDelayed(scrollRunnable, 100)
    }

    override fun onStop() {
        super.onStop()
        mHandler.removeCallbacks(scrollRunnable)
    }


}