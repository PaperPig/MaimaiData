package com.paperpig.maimaidata.ui.songdetail

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.databinding.ActivityPinchImageBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.utils.PermissionHelper
import com.paperpig.maimaidata.utils.PictureUtils
import com.paperpig.maimaidata.utils.setDebouncedClickListener
import kotlinx.coroutines.launch

class PinchImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinchImageBinding

    private lateinit var permissionHelper: PermissionHelper

    private var coverDrawable: Drawable? = null

    val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            permissionHelper.onRequestPermissionsResult(result)
        }

    companion object {
        const val IMAGE_URL = "url"
        const val COVER_ID = "id"

        fun actionStart(context: Context, url: String, id: String, bundle: Bundle) {
            val intent = Intent(context, PinchImageActivity::class.java).apply {
                putExtra(IMAGE_URL, url)
                putExtra(COVER_ID, id)
            }
            context.startActivity(intent, bundle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPinchImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        permissionHelper = PermissionHelper.with(this)

        val thumbnailUrl = intent.getStringExtra(IMAGE_URL)

        val largeImageId = intent.getStringExtra(COVER_ID)?.padStart(5, '0')

        val largeCoverUrl = "${MaimaiDataClient.DIVING_FISH_COVER_URL}$largeImageId.png"

        GlideApp.with(this)
            .load(largeCoverUrl)
            .thumbnail(GlideApp.with(this).load(thumbnailUrl))
            .addListener(coverLoadListener())
            .into(binding.pinchImageView)

        //图片保存
        binding.saveCoverBtn.setDebouncedClickListener {
            coverDrawable?.apply {
                permissionHelper.registerLauncher(requestPermissionLauncher)
                    .checkStoragePermission(object :
                        PermissionHelper.PermissionCallback {
                        override fun onAllGranted() {
                            lifecycleScope.launch {
                                PictureUtils.savePicture(
                                    this@PinchImageActivity,
                                    coverDrawable!!.toBitmap(400, 400),
                                    PictureUtils.coverPath,
                                    largeImageId!!
                                )

                            }
                        }

                        override fun onDenied(deniedPermissions: List<String>) {
                            Toast.makeText(
                                this@PinchImageActivity,
                                getString(R.string.storage_permission_denied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    })

            }
        }
        //图片分享
        binding.shareCoverBtn.setDebouncedClickListener {
            coverDrawable?.apply {
                lifecycleScope.launch {
                    PictureUtils.saveCacheBitmap(
                        this@PinchImageActivity,
                        coverDrawable!!.toBitmap(400, 400)
                    )?.apply {
                        val uriForFile = FileProvider.getUriForFile(
                            this@PinchImageActivity,
                            "${application.packageName}.fileprovider",
                            this
                        )

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uriForFile)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        startActivity(
                            Intent.createChooser(
                                shareIntent,
                                getString(R.string.share_cover)
                            )
                        )
                    }

                }
            }

        }

        binding.pinchImageView.setOnClickListener {
            finishAfterTransition()
        }
    }


    /**
     * 网络图片加载监听
     */
    private fun coverLoadListener(): RequestListener<Drawable> {
        return object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable?>?,
                isFirstResource: Boolean
            ): Boolean {
                Toast.makeText(
                    this@PinchImageActivity,
                    R.string.download_cover_error,
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable?>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                //加载完成显示按钮
                binding.coverButtonGroup.visibility = View.VISIBLE
                coverDrawable = resource
                return false
            }
        }
    }


    override fun finishAfterTransition() {
        binding.pinchImageView.reset()
        super.finishAfterTransition()
    }

}