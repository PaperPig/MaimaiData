package com.paperpig.maimaidata.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener3
import com.paperpig.maimaidata.BuildConfig
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityMainBinding
import com.paperpig.maimaidata.model.AppUpdateModel
import com.paperpig.maimaidata.network.MaimaiDataRequests
import com.paperpig.maimaidata.ui.finaletodx.FinaleToDxFragment
import com.paperpig.maimaidata.ui.rating.RatingFragment
import com.paperpig.maimaidata.ui.songlist.SongListFragment
import com.paperpig.maimaidata.utils.SharePreferencesUtils
import io.reactivex.disposables.Disposable

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var finaleToDxFragment: FinaleToDxFragment
    private lateinit var ratingFragment: RatingFragment
    private lateinit var songListFragment: SongListFragment
    private var updateDisposable: Disposable? = null
    private var isUpdateChecked = false
    private var downloadTask: DownloadTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarLayout.toolbar)


        if (savedInstanceState != null) {
            supportActionBar?.title = savedInstanceState.getString("TOOLBAR_TITLE")

            supportFragmentManager.getFragment(
                savedInstanceState, FinaleToDxFragment.TAG
            )?.apply {
                finaleToDxFragment = this as FinaleToDxFragment
            }

            supportFragmentManager.getFragment(
                savedInstanceState, SongListFragment.TAG
            )?.apply {
                songListFragment = this as SongListFragment
            }

            supportFragmentManager.getFragment(
                savedInstanceState, RatingFragment.TAG
            )?.apply {
                ratingFragment = this as RatingFragment
            }
        } else {
            showFragment(R.id.navDXSongList)
        }

        binding.mainBottomNaviView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navDXSongList -> {
                    showFragment(R.id.navDXSongList)
                    true
                }

                R.id.navDxTarget -> {
                    showFragment(R.id.navDxTarget)
                    true
                }

                R.id.navDXScoreTransform -> {
                    showFragment(R.id.navDXScoreTransform)
                    true
                }

                else -> {
                    true
                }
            }
        }
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("TOOLBAR_TITLE", supportActionBar?.title.toString())
        if (::finaleToDxFragment.isInitialized) supportFragmentManager.putFragment(
            outState, FinaleToDxFragment.TAG, finaleToDxFragment
        )
        if (::songListFragment.isInitialized) supportFragmentManager.putFragment(
            outState, SongListFragment.TAG, songListFragment
        )
        if (::ratingFragment.isInitialized) supportFragmentManager.putFragment(
            outState, RatingFragment.TAG, ratingFragment
        )
    }

    override fun onResume() {
        super.onResume()
        if (!isUpdateChecked) {
            updateDisposable?.dispose()
            checkUpdate()
        }
    }

    /**
     * check update
     */
    private fun checkUpdate() {
        updateDisposable = MaimaiDataRequests.fetchUpdateInfo().subscribe({
            isUpdateChecked = true
            if (it.version != null && it.version!! > BuildConfig.VERSION_NAME && !it.url.isNullOrBlank()) {
                MaterialDialog.Builder(this).title(
                    this@MainActivity.getString(
                        R.string.maimai_data_update_title, it.version
                    )
                ).content(
                    (it.info
                        ?: this@MainActivity.getString(R.string.maimai_data_update_default_content)).replace(
                        "\\n", "\n"
                    )
                ).positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel).onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            startActivity(Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse(it.url)
                            })
                        }
                    }.onNegative { d, _ ->
                        d.dismiss()
                    }.autoDismiss(true).cancelable(true).show()
            } else if (SharePreferencesUtils(
                    this, "version"
                ).getDataVersion() < it.dataVersion2!!
            ) {
                MaterialDialog.Builder(this)
                    .title(this@MainActivity.getString(R.string.maimai_data_data_update_title))
                    .content(
                        String.format(
                            this@MainActivity.getString(R.string.maimai_data_data_update_info),
                            SharePreferencesUtils(this, "version").getDataVersion(),
                            it.dataVersion2
                        )
                    ).positiveText(R.string.maimai_data_update_download)
                    .negativeText(R.string.common_cancel).onPositive { _, which ->
                        if (DialogAction.POSITIVE == which) {
                            startDataDownload(it)
                        }
                    }.onNegative { d, _ ->
                        d.dismiss()
                    }.autoDismiss(true).cancelable(true).show()
            }
        }, {
            it.printStackTrace()
        })
    }

    private fun startDataDownload(appUpdateModel: AppUpdateModel) {
        val updateDialog =
            MaterialDialog.Builder(this).title(getString(R.string.maimai_data_download_title))
                .content(getString(R.string.maimai_data_start_download)).cancelable(false).show()
        downloadTask =
            DownloadTask.Builder(
                appUpdateModel.dataUrl2!!,
                filesDir.path,
                "songdata.json"
            )
                .setMinIntervalMillisCallbackProcess(16).setPassIfAlreadyCompleted(false).build()


        downloadTask!!.enqueue(object : DownloadListener3() {
            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
            }

            override fun connected(
                task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
            ) {
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                updateDialog.setContent("$currentOffset/$totalLength")
            }

            override fun started(task: DownloadTask) {
                updateDialog.show()
            }

            override fun completed(task: DownloadTask) {
                updateDialog.dismiss()
                songListFragment.loadData()
                SharePreferencesUtils(
                    this@MainActivity, "version"
                ).setDataVersion(appUpdateModel.dataVersion2!!)
            }

            override fun canceled(task: DownloadTask) {
                updateDialog.dismiss()
            }

            override fun error(task: DownloadTask, e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.maimai_data_download_error),
                    Toast.LENGTH_SHORT
                ).show()
                updateDialog.dismiss()
            }

            override fun warn(task: DownloadTask) {
            }

        })
    }

    private fun showFragment(int: Int) {
        invalidateMenu()
        val ft = supportFragmentManager.beginTransaction()
        hideAllFragment(ft)
        when (int) {
            R.id.navDXScoreTransform -> {
                supportActionBar?.setTitle(R.string.dx_score_transform)
                if (!::finaleToDxFragment.isInitialized) {
                    finaleToDxFragment = FinaleToDxFragment.newInstance()
                    ft.add(R.id.fragment_content, finaleToDxFragment, FinaleToDxFragment.TAG)
                } else {
                    ft.show(finaleToDxFragment)
                }
            }

            R.id.navDxTarget -> {
                supportActionBar?.setTitle(R.string.dx_rating_correlation)
                if (!::ratingFragment.isInitialized) {
                    ratingFragment = RatingFragment.newInstance()
                    ft.add(R.id.fragment_content, ratingFragment, RatingFragment.TAG)
                } else {
                    ft.show(ratingFragment)
                }
            }

            R.id.navDXSongList -> {
                supportActionBar?.setTitle(R.string.dx_song_list)
                if (!::songListFragment.isInitialized) {
                    songListFragment = SongListFragment.newInstance()
                    ft.add(R.id.fragment_content, songListFragment, SongListFragment.TAG)
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