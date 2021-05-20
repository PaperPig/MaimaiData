package com.paperpig.maimaidata.ui

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.FragmentTransaction
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.ui.finaletodx.FinaleToDxFragment
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.title.*

class MainActivity : AppCompatActivity() {
    private lateinit var finaleToDxFragment: FinaleToDxFragment
    private lateinit var ratingFragment: RatingFragment
    private lateinit var songListFragment: SongListFragment


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_menu)
        }

        if (savedInstanceState != null) {
            supportActionBar?.title = savedInstanceState.getString("TOOLBAR_TITLE")

            supportFragmentManager.getFragment(
                savedInstanceState,
                FinaleToDxFragment.TAG
            )?.apply {
                finaleToDxFragment = this as FinaleToDxFragment
            }

            supportFragmentManager.getFragment(
                savedInstanceState,
                SongListFragment.TAG
            )?.apply {
                songListFragment = this as SongListFragment
            }

            supportFragmentManager.getFragment(
                savedInstanceState,
                RatingFragment.TAG
            )?.apply {
                ratingFragment = this as RatingFragment
            }
        } else {
            navView.setCheckedItem(R.id.navDXSongList)
            showFragment(R.id.navDXSongList)
        }



        navView.setNavigationItemSelectedListener {
            showFragment(it.itemId)
            drawerLayout.closeDrawers()
            true
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("TOOLBAR_TITLE", supportActionBar?.title.toString())
        if (::finaleToDxFragment.isInitialized) supportFragmentManager.putFragment(
                outState,
                FinaleToDxFragment.TAG,
                finaleToDxFragment
        )
        if (::songListFragment.isInitialized) supportFragmentManager.putFragment(
            outState,
            SongListFragment.TAG,
            songListFragment
        )
        if (::ratingFragment.isInitialized) supportFragmentManager.putFragment(
            outState,
            RatingFragment.TAG,
            ratingFragment
        )
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                drawerLayout.openDrawer(GravityCompat.START)
        }

        return false
    }


    private fun showFragment(int: Int) {
        val ft = supportFragmentManager.beginTransaction()
        hideAllFragment(ft)
        when (int) {
            R.id.navDXScoreTransform -> {
                supportActionBar?.setTitle(R.string.dx_score_transform)
                if (!::finaleToDxFragment.isInitialized) {
                    finaleToDxFragment = FinaleToDxFragment.newInstance()
                    ft.add(R.id.fragmentContent, finaleToDxFragment, FinaleToDxFragment.TAG)
                } else {
                    ft.show(finaleToDxFragment)
                }
            }
            R.id.navDxTarget -> {
                supportActionBar?.setTitle(R.string.dx_rating_correlation)
                if (!::ratingFragment.isInitialized) {
                    ratingFragment = RatingFragment.newInstance()
                    ft.add(R.id.fragmentContent, ratingFragment, RatingFragment.TAG)
                } else {
                    ft.show(ratingFragment)
                }
            }

            R.id.navDXSongList -> {
                supportActionBar?.setTitle(R.string.dx_song_list)
                if (!::songListFragment.isInitialized) {
                    songListFragment = SongListFragment.newInstance()
                    ft.add(R.id.fragmentContent, songListFragment, SongListFragment.TAG)
                } else {
                    ft.show(songListFragment)
                }

            }
        }
        ft.commit()
    }

    private fun hideAllFragment(ft: FragmentTransaction) {
        ft.apply {
            if (::finaleToDxFragment.isInitialized) {
                hide(finaleToDxFragment)
            }
            if (::ratingFragment.isInitialized) {
                hide(ratingFragment)
            }
            if (::songListFragment.isInitialized) {
                hide(songListFragment)
            }
        }
    }
}