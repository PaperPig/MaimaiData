package com.paperpig.maimaidata.ui.songdetail

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.LinearInterpolator
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
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivitySongDetailBinding
import com.paperpig.maimaidata.db.AppDataBase
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.repository.AliasRepository
import com.paperpig.maimaidata.repository.RecordRepository
import com.paperpig.maimaidata.utils.Constants
import com.paperpig.maimaidata.utils.SpUtil
import com.paperpig.maimaidata.utils.TtsManager
import com.paperpig.maimaidata.utils.setCopyOnLongClick
import com.paperpig.maimaidata.utils.setShrinkOnTouch
import com.paperpig.maimaidata.utils.toDp
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class SongDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySongDetailBinding

    private lateinit var data: SongWithChartsEntity

    // 异步准备音频，并统一处理解码或播放失败。
    private val mediaPlayer = MediaPlayer().apply {
        setOnPreparedListener {
            it.start()
            updateTtsState(TtsState.PLAYING)
        }
        setOnCompletionListener { updateTtsState(TtsState.IDLE) }
        setOnErrorListener { player, what, extra ->
            Log.e("TTS", "Audio playback failed: $what, $extra")
            player.reset()
            showTtsError()
            true
        }
    }
    private var ttsJob: Job? = null
    private var ttsAnimator: ObjectAnimator? = null
    private var ttsState = TtsState.IDLE

    // 区分语音准备和实际播放，以便按钮显示对应反馈。
    private enum class TtsState { IDLE, LOADING, PLAYING }

    companion object {
        const val EXTRA_DATA_KEY = "data"

        fun actionStart(context: Context, songWithChartsEntity: SongWithChartsEntity) {
            val intent = Intent(context, SongDetailActivity::class.java).apply {
                putExtra(EXTRA_DATA_KEY, songWithChartsEntity)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
        binding = ActivitySongDetailBinding.inflate(layoutInflater)

        with(binding) {
            setContentView(root)

            setSupportActionBar(toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setDisplayShowHomeEnabled(true)
            }

            data = intent.getParcelableExtra<SongWithChartsEntity>(EXTRA_DATA_KEY)!!
            val songData = data.songData

            //设置背景颜色
            appbarLayout.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.bgColor
                )
            )
            tabLayout.apply {
                setSelectedTabIndicatorColor(
                    ContextCompat.getColor(
                        this@SongDetailActivity,
                        songData.bgColor
                    )
                )
                setTabTextColors(
                    Color.BLACK, ContextCompat.getColor(
                        this@SongDetailActivity,
                        songData.bgColor
                    )
                )
            }
            toolbarLayout.setContentScrimResource(songData.bgColor)
            GlideApp.with(this@SongDetailActivity)
                .load(MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl)
                .into(songJacket)
            songJacket.setBackgroundColor(
                ContextCompat.getColor(
                    this@SongDetailActivity,
                    songData.strokeColor
                )
            )

            //显示歌曲信息
            songTitle.apply {
                text = songData.title

                setShrinkOnTouch()
                setCopyOnLongClick(songData.title)
            }

            songIdText.apply {
                text = songData.id.toString()

                setShrinkOnTouch()
                setCopyOnLongClick(songData.id.toString())
            }

            songArtist.text = songData.artist
            songBpm.text = songData.bpm.toString()
            songGenre.text = songData.genre
            GlideApp.with(this@SongDetailActivity).apply {
                if (songData.type == Constants.CHART_TYPE_DX) {
                    load(R.drawable.ic_deluxe).into(binding.songType)
                } else {
                    load(R.drawable.ic_standard).into(binding.songType)
                }
            }
            setVersionImage(songAddVersion, songData.version)
            setCnVersionImage(songAddCnVersion, songData.from)

            // 同一按钮负责播放和取消，加载期间也能立即停止。
            ttsPlayButton.setOnClickListener {
                if (ttsState == TtsState.IDLE) {
                    playTTS(songData.reading)
                } else {
                    stopTts()
                }
            }


            //显示别名
            if (Settings.getEnableShowAlias()) {
                AliasRepository.getInstance(AppDataBase.getInstance().aliasDao())
                    .getAliasListBySongId(songData.id).observe(this@SongDetailActivity) {
                        //对添加的别名进行flow约束
                        if (it.isNotEmpty()) {
                            val aliasViewIds = songAliasFlow.referencedIds.toMutableList()
                            it.forEachIndexed { _, item ->
                                val textView = TextView(this@SongDetailActivity).apply {
                                    text = item.alias
                                    id = View.generateViewId()
                                    aliasViewIds.add(id)
                                    val padding = 5.toDp().toInt()
                                    setPadding(padding, padding, padding, padding)
                                    setBackgroundResource(R.drawable.mmd_song_alias_info_bg)
                                    setTextColor(
                                        ContextCompat.getColor(
                                            this@SongDetailActivity,
                                            songData.bgColor
                                        )
                                    )
                                    layoutParams = ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                                    )

                                    setShrinkOnTouch()
                                    setCopyOnLongClick(item.alias)
                                }
                                constraintLayout.addView(textView)
                            }
                            songAliasFlow.referencedIds = aliasViewIds.toIntArray()
                        } else {
                            aliasLabel.visibility = View.GONE
                        }
                    }
            } else {
                aliasLabel.visibility = View.GONE
            }

            //打开歌曲大图
            songJacket.setOnClickListener {
                val options: ActivityOptions = ActivityOptions
                    .makeSceneTransitionAnimation(
                        this@SongDetailActivity,
                        binding.songJacket,
                        "shared_image"
                    )
                PinchImageActivity.actionStart(
                    this@SongDetailActivity,
                    MaimaiDataClient.IMAGE_BASE_URL + songData.imageUrl,
                    songData.id.toString(),
                    options.toBundle()
                )
            }

            //设置收藏
            favButton.apply {
                val colorFilter: (Boolean) -> Int = { isFavor: Boolean ->
                    if (isFavor) {
                        0
                    } else {
                        Color.WHITE
                    }
                }
                setColorFilter(colorFilter.invoke(SpUtil.isFavorite(songData.id.toString())))
                setOnClickListener {
                    val isFavor = SpUtil.isFavorite(songData.id.toString())
                    SpUtil.setFavorite(songData.id.toString(), !isFavor)
                    setColorFilter(colorFilter.invoke(!isFavor))
                }
            }

            //获取成绩数据
            RecordRepository.getInstance(AppDataBase.getInstance().recordDao())
                .getRecordsBySongId(songData.id).observe(this@SongDetailActivity) {
                    setupFragments(it)
                }
        }
    }

    private fun setupFragments(recordList: List<RecordEntity>) {
        val list = ArrayList<Fragment>()

        (1..data.charts.size).forEach { i ->
            val position = data.charts.size - i
            list.add(SongLevelFragment.newInstance(data, position, recordList.find {
                it.songId == data.songData.id &&
                        it.levelIndex == position
            }))
        }

        binding.viewPager.adapter = LevelDataFragmentAdapter(supportFragmentManager, -1, list)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    // 仅播放最近一次点击的曲名，避免较慢的旧请求覆盖新请求。
    private fun playTTS(content: String) {
        if (content.isBlank()) return
        ttsJob?.cancel()
        mediaPlayer.reset()
        updateTtsState(TtsState.LOADING)
        ttsJob = lifecycleScope.launch {
            try {
                val transTTSFilePath = TtsManager.transTTS(content)
                mediaPlayer.setDataSource(transTTSFilePath)
                mediaPlayer.prepareAsync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("TTS", "Failed to play song title", e)
                mediaPlayer.reset()
                showTtsError()
            }
        }
    }

    // 提示网络或音频异常，允许用户重新点击播放。
    private fun showTtsError() {
        updateTtsState(TtsState.IDLE)
        Toast.makeText(this, R.string.tts_play_failed, Toast.LENGTH_SHORT).show()
    }

    // 仅对图标执行加载和声波动画，保持按钮底座稳定，减少视觉干扰。
    private fun updateTtsState(state: TtsState) {
        ttsState = state
        ttsAnimator?.cancel()
        ttsAnimator = null
        (binding.ttsPlayButton.drawable as? Animatable)?.stop()
        binding.ttsPlayButton.apply {
            val icon = when (state) {
                TtsState.IDLE -> R.drawable.mmd_ic_tts_play
                TtsState.LOADING -> R.drawable.mmd_ic_tts_loading
                TtsState.PLAYING -> R.drawable.mmd_ic_tts_playing
            }
            setImageResource(icon)
            contentDescription = getString(when (state) {
                TtsState.IDLE -> R.string.play_title
                TtsState.LOADING -> R.string.tts_loading
                TtsState.PLAYING -> R.string.tts_stop
            })
            ttsAnimator = when (state) {
                TtsState.IDLE -> null
                TtsState.LOADING -> ObjectAnimator.ofInt(drawable, "level", 0, 10000).apply {
                    duration = 1000
                    interpolator = LinearInterpolator()
                    repeatCount = ValueAnimator.INFINITE
                }
                TtsState.PLAYING -> null
            }
            ttsAnimator?.start()
            (drawable as? Animatable)?.start()
        }
    }

    // 取消合成和播放，统一清理按钮动效。
    private fun stopTts() {
        ttsJob?.cancel()
        mediaPlayer.reset()
        updateTtsState(TtsState.IDLE)
    }

    // 离开详情页时停止语音，防止后台请求完成后突然播放。
    override fun onStop() {
        stopTts()
        super.onStop()
    }

    // 释放原生播放器，避免多次打开详情页积累音频资源。
    override fun onDestroy() {
        mediaPlayer.release()
        super.onDestroy()
    }


    class LevelDataFragmentAdapter(
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
                startsWith("250") -> versionDrawable = R.drawable.maimaidx_prism
                startsWith("255") -> versionDrawable = R.drawable.maimaidx_prism_plus
                startsWith("260") -> versionDrawable = R.drawable.maimaidx_circle
                // 为 CiRCLE PLUS 收录曲目显示对应的日文版本标识。
                startsWith("265") -> versionDrawable = R.drawable.maimaidx_circle_plus
                // 为 MAGiCAL 收录曲目显示对应的日文版本标识。
                startsWith("270") -> versionDrawable = R.drawable.maimaidx_magical
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
                equals("舞萌DX 2025") -> versionDrawable = R.drawable.maimaidx_2025
                equals("舞萌DX 2026") -> versionDrawable = R.drawable.maimaidx_2026
            }
        }
        Glide.with(view.context)
            .load(versionDrawable)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}
