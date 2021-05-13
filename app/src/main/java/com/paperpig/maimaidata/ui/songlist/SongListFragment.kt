package com.paperpig.maimaidata.ui.songlist

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.WindowsUtils
import kotlinx.android.synthetic.main.fragment_song_list.*
import kotlinx.android.synthetic.main.layout_song_search.*
import kotlinx.android.synthetic.main.search_bar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.hypot


class SongListFragment : BaseFragment() {
    private lateinit var songAdapter: SongListAdapter

    companion object {

        @JvmStatic
        fun newInstance() =
            SongListFragment()

        const val TAG = "SongListFragment"
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_song_list, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupAnimation()

        songListRecyclerView.apply {
            songAdapter = SongListAdapter()
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

        closeLayout.setOnClickListener {
            showOrHideSearchBar()
        }

        searchButton.setOnClickListener {
            songAdapter.search(
                searchText.text.toString(),
                getSortCheck(),
                getVersionCheck(),
                levelSpinner.selectedItem.toString(),
                sortSpinner.selectedItem.toString()
            )
            hideKeyboard(it)
            showOrHideSearchBar()
        }

        searchText.setOnEditorActionListener { _, i, _ ->
            when (i) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    songAdapter.search(
                        searchText.text.toString(),
                        getSortCheck(),
                        getVersionCheck(),
                        levelSpinner.selectedItem.toString(),
                        sortSpinner.selectedItem.toString()
                    )
                    showOrHideSearchBar()
                }
            }
            false
        }


        CoroutineScope(Dispatchers.Main).launch {
            songAdapter.setData(getData())
        }

    }

    private suspend fun getData(): List<SongData> {
        return withContext(Dispatchers.IO) {
            val song2021 = context?.assets?.open("music_data.json")?.bufferedReader()
                    .use { it?.readText() }
            Gson().fromJson<List<SongData>>(
                song2021, object : TypeToken<List<SongData>>() {}.type
            )
        }
    }


    private fun getSortCheck(): List<String> {
        val checkBoxList =
            listOf<CheckBox>(
                popCheck,
                nicoCheck,
                touhouCheck,
                gameVarietyCheck,
                maimaiSortCheck,
                ongekiChuniCheck
            )
        val sortList = mutableListOf<String>()
        for (cb in checkBoxList) {
            if (cb.isChecked) sortList.add(cb.text.toString())
        }
        return sortList
    }


    private fun getVersionCheck(): List<String> {
        val checkBoxList = listOf<CheckBox>(
            maimaiCheck,
            greenCheck,
            orangeCheck,
            pinkCheck,
            murasakiCheck,
            milkCheck,
            finaleCheck,
            dxCheck,
            dx2021Check
        )
        val versionList = mutableListOf<String>()
        for (cb in checkBoxList) {
            if (cb.isChecked) versionList.add(cb.text.toString())
        }
        return versionList
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                showOrHideSearchBar()
                hideKeyboard(view)
            }

        }
        return super.onOptionsItemSelected(item)
    }

    private fun showOrHideSearchBar() {
        val windowWidth = WindowsUtils.getWindowWidth(context)
        val radius =
            hypot(searchLayout.width.toDouble(), searchLayout.height.toDouble()).toFloat()

        if (searchLayout.isVisible) {
            val createCircularReveal = ViewAnimationUtils.createCircularReveal(
                searchLayout, windowWidth.toInt(), 0, radius, 0f
            )
            createCircularReveal.setDuration(300).doOnEnd {
                searchLayout.visibility = View.GONE
            }
            createCircularReveal.start()

        } else {
            val createCircularReveal = ViewAnimationUtils.createCircularReveal(
                searchLayout, windowWidth.toInt(), 0, 0f, radius
            )
            createCircularReveal.setDuration(300).doOnStart {
                searchLayout.visibility = View.VISIBLE
            }
            createCircularReveal.start()
        }
    }

    private fun setupAnimation() {
        val translationAnimatorSet = AnimatorSet()
        translationAnimatorSet.playTogether(
            ObjectAnimator.ofFloat(bgGreen, "translationY", -20f, 20f, -20f).apply {
                duration = 10000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgYellow, "translationY", -20f, 20f, -20f).apply {
                duration = 120000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgBlue, "translationY", -20f, 20f, -20f).apply {
                duration = 8000L
                repeatCount = ValueAnimator.INFINITE
            },
            ObjectAnimator.ofFloat(bgOrange, "translationY", -20f, 20f, -20f).apply {
                duration = 7000L
                repeatCount = ValueAnimator.INFINITE
            }
        )

        translationAnimatorSet.start()
    }


}