package com.paperpig.maimaidata.ui.songdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.paperpig.maimaidata.databinding.ActivityPinchImageBinding
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient

class PinchImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinchImageBinding

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

        val thumbnailUrl = intent.getStringExtra(IMAGE_URL)

        val largeImageId = intent.getStringExtra(COVER_ID)?.padStart(5, '0')

        val largeCoverUrl = "${MaimaiDataClient.DIVING_FISH_COVER_URL}$largeImageId.png"

        GlideApp.with(this).load(largeCoverUrl).thumbnail(
            GlideApp.with(this)
                .load(thumbnailUrl)
        ).into(binding.pinchImageView)


        binding.pinchImageView.setOnClickListener {
            finishAfterTransition()
        }
    }


    override fun finishAfterTransition() {
        binding.pinchImageView.reset()
        super.finishAfterTransition()
    }

}