package com.paperpig.maimaidata.ui.songdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.constat.Constant
import com.paperpig.maimaidata.model.SongData
import kotlinx.android.synthetic.main.activity_song_detail.*

class SongDetailActivity : AppCompatActivity() {

    companion object {
        fun actionStart(context: Context, songData: SongData) {
            val intent = Intent(context, SongDetailActivity::class.java).apply {
                putExtra("songData", songData)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_song_detail)

        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }


        val songData = intent.getSerializableExtra("songData") as SongData

        topLayout.setBackgroundColor(ContextCompat.getColor(this, songData.getBgColor()))
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, songData.getBgColor()))

        Glide.with(this).load(Constant.IMAGE_BASE_URL + songData.basic_info.image_url)
            .into(songJacket)

        songTitle.text = songData.basic_info.title
        songArtist.text = songData.basic_info.artist
        songBpm.text = songData.basic_info.bpm.toString()
        songGenre.text = songData.basic_info.genre
        setVersionImage(songAddVersion, songData.basic_info.from)

        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, songData.getBgColor()))

        val list = ArrayList<Fragment>()
        list.add(SongLevelFragment.newInstance(songData, 0))
        list.add(SongLevelFragment.newInstance(songData, 1))
        list.add(SongLevelFragment.newInstance(songData, 2))
        list.add(SongLevelFragment.newInstance(songData, 3))

        if (songData.level.size == 5)
            list.add(SongLevelFragment.newInstance(songData, 4))
        viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
        tabLayout.setupWithViewPager(viewPager)

    }


    inner class LevelDataFragmentAdapter(
        fragmentManager: FragmentManager,
        behavior: Int,
        private val list: List<Fragment>
    ) :
        FragmentPagerAdapter(fragmentManager, behavior) {
        private val titleArray = arrayOf("BAS", "ADV", "EXP", "MAS", "Re:MAS")

        override fun getItem(position: Int): Fragment {
            return list[position]
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return titleArray[position]
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home ->
                finish()
        }
        return true
    }

    private fun setVersionImage(view: ImageView, addVersion: String) {

        @DrawableRes var versionDrawable = 0
        when (addVersion) {
            "maimai" -> versionDrawable = R.drawable.maimai
            "maimai PLUS" -> versionDrawable = R.drawable.maimai_plus
            "maimai GreeN" -> versionDrawable = R.drawable.maimai_green
            "maimai GreeN PLUS" -> versionDrawable = R.drawable.maimai_green_plus
            "maimai ORANGE" -> versionDrawable = R.drawable.maimai_orange
            "maimai ORANGE PLUS" -> versionDrawable = R.drawable.maimai_orange_plus
            "maimai PiNK" -> versionDrawable = R.drawable.maimai_pink
            "maimai PiNK PLUS" -> versionDrawable = R.drawable.maimai_pink_plus
            "maimai MURASAKi" -> versionDrawable = R.drawable.maimai_murasaki
            "maimai MURASAKi PLUS" -> versionDrawable = R.drawable.maimai_murasaki_plus
            "maimai MiLK" -> versionDrawable = R.drawable.maimai_milk
            "MiLK PLUS" -> versionDrawable = R.drawable.maimai_milk_plus
            "maimai FiNALE" -> versionDrawable = R.drawable.maimai_finale
            "maimai でらっくす" -> versionDrawable = R.drawable.maimaidx
            "maimai でらっくす PLUS" -> versionDrawable = R.drawable.maimaidx_plus
            "maimai でらっくす Splash" -> versionDrawable = R.drawable.maimaidx_splash
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}