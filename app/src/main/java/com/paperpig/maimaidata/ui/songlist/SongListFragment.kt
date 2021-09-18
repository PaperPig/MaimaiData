package com.paperpig.maimaidata.ui.songlist

import android.os.Bundle
import android.os.Handler
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.CheckBox
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.model.SongListModel
import com.paperpig.maimaidata.ui.BaseFragment
import com.paperpig.maimaidata.utils.WindowsUtils
import kotlinx.android.synthetic.main.fragment_song_list.*
import kotlinx.android.synthetic.main.layout_song_search.*
import kotlinx.android.synthetic.main.mmd_universe_style_bg_layout.*
import kotlinx.android.synthetic.main.search_bar.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.hypot


class SongListFragment : BaseFragment() {
    private lateinit var songAdapter: SongListAdapter
    private val mHandler: Handler = Handler()
    private val scrollRunnable: Runnable by lazy {
        object : Runnable {
            override fun run() {
                dosTopRecyclerView.scrollBy(1, 0)
                dosUnderRecyclerView.scrollBy(1, 0)
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
            songAdapter.setData(SongListModel().getData(context)
                .sortedByDescending { it.id.toInt() })
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
        val topLayoutManager = LinearLayoutManager(context)
        topLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        dosTopRecyclerView.apply {
            layoutManager = topLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_top)
        }
        val underLayoutManager = LinearLayoutManager(context)
        underLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        dosUnderRecyclerView.apply {
            layoutManager = underLayoutManager
            adapter = DotsScrollAdapter(context, R.drawable.mmd_home_elem_dots_under)
        }
        mHandler.postDelayed(scrollRunnable, 100)

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