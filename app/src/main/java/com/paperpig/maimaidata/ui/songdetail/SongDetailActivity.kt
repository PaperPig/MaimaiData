package com.paperpig.maimaidata.ui.songdetail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.repository.RecordDataManager
import com.paperpig.maimaidata.repository.SongDataManager
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import com.paperpig.maimaidata.utils.toDp
import com.paperpig.maimaidata.widgets.Settings

class SongDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongDetailBinding
    private val spUtils = SharePreferencesUtils(MaimaiDataApplication.instance, "songInfo")

    companion object {
        const val EXTRA_SONG_ID = "extra_song_id"

        fun actionStart(context: Context, songId: String) {
            val intent = Intent(context, SongDetailActivity::class.java).apply {
                putExtra(EXTRA_SONG_ID, songId)
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

            val songId: String? = intent.getStringExtra(EXTRA_SONG_ID)
            SongDataManager.list.find { it.id == songId }?.let { songData ->
                val recordList = RecordDataManager.list

                appbarLayout.setBackgroundColor(
                    ContextCompat.getColor(
                        this@SongDetailActivity,
                        songData.getBgColor()
                    )
                )
                tabLayout.apply {
                    setSelectedTabIndicatorColor(
                        ContextCompat.getColor(
                            this@SongDetailActivity,
                            songData.getBgColor()
                        )
                    )
                    setTabTextColors(
                        Color.BLACK, ContextCompat.getColor(
                            this@SongDetailActivity,
                            songData.getBgColor()
                        )
                    )
                }

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

                songTitle.apply {
                    text = songData.basic_info.title

                    setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                v.animate()
                                    .scaleX(0.9f)
                                    .scaleY(0.9f)
                                    .setDuration(100)
                                    .start()
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                v.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(100)
                                    .start()
                            }
                        }
                        false // 保留 long click 事件
                    }

                    setOnLongClickListener {
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        if (clipboard != null) {
                            val clip = ClipData.newPlainText("Alias", songData.basic_info.title)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "已复制别名：${songData.basic_info.title}", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "无法访问剪贴板", Toast.LENGTH_SHORT).show()
                        }
                        true
                    }
                }
                songArtist.text = songData.basic_info.artist
                songBpm.text = songData.basic_info.bpm.toString()
                songGenre.text = songData.basic_info.genre
                GlideApp.with(this@SongDetailActivity).apply {
                    if (songData.type == Constants.CHART_TYPE_DX) {
                        load(R.drawable.ic_deluxe).into(binding.songType)
                    } else {
                        load(R.drawable.ic_standard).into(binding.songType)
                    }
                }
                setVersionImage(songAddVersion, songData.basic_info.version)
                setCnVersionImage(songAddCnVersion, songData.basic_info.from)

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

                if (Settings.getEnableShowAlias()) {
                    //对添加的别名进行flow约束
                    val aliasViewIds = songAliasFlow.referencedIds.toMutableList()
                    songData.alias?.forEachIndexed { _, item ->
                        val textView = TextView(this@SongDetailActivity).apply {
                            text = item
                            id = View.generateViewId()
                            aliasViewIds.add(id)
                            val padding = 5.toDp().toInt()
                            setPadding(padding, padding, padding, padding)
                            setBackgroundResource(R.drawable.mmd_song_alias_info_bg)
                            setTextColor(
                                ContextCompat.getColor(
                                    this@SongDetailActivity,
                                    songData.getBgColor()
                                )
                            )
                            layoutParams = ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT
                            )

                            setOnTouchListener { v, event ->
                                when (event.action) {
                                    MotionEvent.ACTION_DOWN -> {
                                        v.animate()
                                            .scaleX(0.9f)
                                            .scaleY(0.9f)
                                            .setDuration(100)
                                            .start()
                                    }
                                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                        v.animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(100)
                                            .start()
                                    }
                                }
                                false // 保留 long click 事件
                            }

                            setOnLongClickListener {
                                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                if (clipboard != null) {
                                    val clip = ClipData.newPlainText("Alias", item)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "已复制别名：$item", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "无法访问剪贴板", Toast.LENGTH_SHORT).show()
                                }
                                true
                            }
                        }
                        constraintLayout.addView(textView)
                    } ?: run {
                        aliasLabel.visibility = View.GONE
                    }
                    songAliasFlow.referencedIds = aliasViewIds.toIntArray()
                } else {
                    aliasLabel.visibility = View.GONE
                }


                val list = ArrayList<Fragment>()

                (1..songData.level.size).forEach { i ->
                    val position = songData.level.size - i
                    list.add(SongLevelFragment.newInstance(songData, position, recordList.find {
                        it.song_id == songData.id &&
                                it.level_index == position
                    }))
                }

                viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
                tabLayout.setupWithViewPager(viewPager)
            }
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
            return when (count) {
                //1个难度显示标签为宴会場
                1 -> arrayOf("宴会場")[position]
                //2个难度显示便签为宴会場1p和2p
                2 -> arrayOf("1p", "2p")[position]
                4 -> arrayOf("MAS", "EXP", "ADV", "BAS")[position]
                else -> arrayOf("Re:MAS", "MAS", "EXP", "ADV", "BAS")[position]
            }
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
                startsWith("235") -> versionDrawable = R.drawable.maimaidx_festival_plus
                startsWith("240") -> versionDrawable = R.drawable.maimaidx_buddies
                startsWith("245") -> versionDrawable = R.drawable.maimaidx_buddies_plus
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }

    private fun setCnVersionImage(view: ImageView, addVersion: String) {

        @DrawableRes var versionDrawable = 0
        with(addVersion) {
            when {
                equals("舞萌DX") -> versionDrawable = R.drawable.maimaidx_cn
                equals("舞萌DX 2021") -> versionDrawable = R.drawable.maimaidx_2021
                equals("舞萌DX 2022") -> versionDrawable = R.drawable.maimaidx_2022
                equals("舞萌DX 2023") -> versionDrawable = R.drawable.maimaidx_2023
                equals("舞萌DX 2024") -> versionDrawable = R.drawable.maimaidx_2024
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}