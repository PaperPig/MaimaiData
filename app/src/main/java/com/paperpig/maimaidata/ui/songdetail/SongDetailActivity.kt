package com.paperpig.maimaidata.ui.songdetail

import android.content.Context
import android.content.Intent
import android.graphics.Color
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
import com.paperpig.maimaidata.MaimaiDataApplication
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivitySongDetailBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.utils.MaimaiRecordUtils
import com.paperpig.maimaidata.utils.SharePreferencesUtils

class SongDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongDetailBinding
    private val spUtils = SharePreferencesUtils(MaimaiDataApplication.instance, "songInfo")

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
        binding = ActivitySongDetailBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)

            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            val songData: SongData = intent.getParcelableExtra("songData")!!
            val record = MaimaiRecordUtils.getRecord(this@SongDetailActivity)

            topLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.getBgColor()
                )
            )
            tabLayout.setSelectedTabIndicatorColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.getBgColor()
                )
            )
            toolbarLayout.setContentScrimResource(songData.getBgColor())

            GlideApp.with(this@SongDetailActivity)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.basic_info.image_url)
                .into(songJacket)

            songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.getStrokeColor()
                )
            )

            songTitle.text = songData.basic_info.title
            songArtist.text = songData.basic_info.artist
            songBpm.text = songData.basic_info.bpm.toString()
            songGenre.text = songData.basic_info.genre
            setVersionImage(songAddVersion, songData.basic_info.version)

            val colorFilter: (Boolean) -> Int = { isFavor: Boolean ->
                if (isFavor) {
                    0
                } else {
                    Color.WHITE
                }
            }
            favButton.apply {
                setColorFilter(colorFilter.invoke(spUtils.isFavorite(songData.id)))
                setOnClickListener {
                    val isFavor = spUtils.isFavorite(songData.id)
                    spUtils.setFavorite(songData.id, !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            tabLayout.setSelectedTabIndicatorColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.getBgColor()
                )
            )

            val list = ArrayList<Fragment>()

            (1..songData.level.size).forEach { i ->
                val position = songData.level.size - i
                list.add(SongLevelFragment.newInstance(songData, position, record?.find {
                    it.title == songData.basic_info.title &&
                            it.level_index == position
                }))
            }

            viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
            tabLayout.setupWithViewPager(binding.viewPager)
        }
    }


    inner class LevelDataFragmentAdapter(
        fragmentManager: FragmentManager,
        behavior: Int,
        private val list: List<Fragment>
    ) :
        FragmentPagerAdapter(fragmentManager, behavior) {

        override fun getItem(position: Int): Fragment {
            return list[position]
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return if (count == 4) arrayOf("MAS", "EXP", "ADV", "BAS")[position]
            else arrayOf("Re:MAS", "MAS", "EXP", "ADV", "BAS")[position]
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
        with(addVersion) {
            when {
                startsWith("100") -> versionDrawable = R.drawable.maimai
                startsWith("110") -> versionDrawable = R.drawable.maimai_plus
                startsWith("120") -> versionDrawable = R.drawable.maimai_green
                startsWith("130") -> versionDrawable = R.drawable.maimai_green_plus
                startsWith("140") -> versionDrawable = R.drawable.maimai_orange
                startsWith("150") -> versionDrawable = R.drawable.maimai_orange_plus
                startsWith("160") -> versionDrawable = R.drawable.maimai_pink
                startsWith("170") -> versionDrawable = R.drawable.maimai_pink_plus
                startsWith("180") -> versionDrawable = R.drawable.maimai_murasaki
                startsWith("185") -> versionDrawable = R.drawable.maimai_murasaki_plus
                startsWith("190") -> versionDrawable = R.drawable.maimai_milk
                startsWith("195") -> versionDrawable = R.drawable.maimai_milk_plus
                startsWith("199") -> versionDrawable = R.drawable.maimai_finale
                startsWith("200") -> versionDrawable = R.drawable.maimaidx
                startsWith("205") -> versionDrawable = R.drawable.maimaidx_plus
                startsWith("210") -> versionDrawable = R.drawable.maimaidx_splash
                startsWith("215") -> versionDrawable = R.drawable.maimaidx_splash_plus
                startsWith("220") -> versionDrawable = R.drawable.maimaidx_universe
                startsWith("225") -> versionDrawable = R.drawable.maimaidx_universe_plus
                startsWith("230") -> versionDrawable = R.drawable.maimaidx_festival
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}