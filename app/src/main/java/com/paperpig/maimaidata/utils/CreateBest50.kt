package com.paperpig.maimaidata.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.db.entity.RecordEntity
import com.paperpig.maimaidata.db.entity.SongWithChartsEntity
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.network.MaimaiDataClient
import com.paperpig.maimaidata.widgets.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors


object CreateBest50 {

    private const val ITEM_WIDTH = 200
    private const val ITEM_HEIGHT = 250
    private const val ITEM_PADDING = 10
    private const val VERSION_PADDING = 50
    private const val HEADER_HEIGHT = 250
    private const val CONTAINER_WIDTH = ITEM_WIDTH * 10 + ITEM_PADDING * 10 + VERSION_PADDING
    private const val CONTAINER_HEIGHT = ITEM_HEIGHT * 5 + ITEM_PADDING * 5


    @SuppressLint("DiscouragedApi")
    suspend fun createSongInfo(
        context: Activity,
        dataList: List<SongWithChartsEntity>,
        old: List<RecordEntity>,
        new: List<RecordEntity>
    ) {
        if (old.isEmpty() && new.isEmpty()) {
            Toast.makeText(context, "尚未取得歌曲信息，请稍后重试", Toast.LENGTH_SHORT)
                .show()
            return
        }
        withContext(Dispatchers.IO) {
            val containerBitmap = drawableToBitmap(
                context, R.drawable.mmd_player_best50, CONTAINER_WIDTH,
                CONTAINER_HEIGHT + HEADER_HEIGHT
            ).copy(Bitmap.Config.ARGB_8888, true)
            val containerCanvas = Canvas(containerBitmap)
            val textPaint = TextPaint()


            // 绘制rating数据图
            val mainBitmap =
                createBitmap(CONTAINER_WIDTH, CONTAINER_HEIGHT)
            val canvas = Canvas(mainBitmap)

            val threadPool = Executors.newFixedThreadPool(5)

            // 绘制旧版本乐曲
            for (i in old.indices) {
                println(Thread.currentThread().name)
                threadPool.submit {
                    println(Thread.currentThread().name)
                    val drawBitmap = drawSongItem(
                        context,
                        old[i], dataList
                    )
                    canvas.drawBitmap(
                        drawBitmap,
                        (i % 7 * ITEM_WIDTH + i % 7 * ITEM_PADDING).toFloat(),
                        (i / 7 * ITEM_HEIGHT + i / 7 * ITEM_PADDING).toFloat(),
                        null
                    )
                }.get()
            }

            // 绘制现行版本乐曲
            for (i in new.indices) {
                println(Thread.currentThread().name)

                threadPool.submit {
                    val drawBitmap = drawSongItem(context, new[i], dataList)
                    canvas.drawBitmap(
                        drawBitmap,
                        (i % 3 * ITEM_WIDTH + (ITEM_WIDTH + ITEM_PADDING) * 7 + VERSION_PADDING + i % 3 * ITEM_PADDING).toFloat(),
                        (i / 3 * ITEM_HEIGHT + i / 3 * ITEM_PADDING).toFloat(),
                        null
                    )
                }.get()
            }

            threadPool.shutdown()



            containerCanvas.drawBitmap(mainBitmap, 0f, HEADER_HEIGHT.toFloat(), null)

            //绘制rating板
            val ratingPlateBitmap =
                getRatingPlate(context, (old.sumOf { it.ra } + new.sumOf { it.ra }))
            containerCanvas.drawBitmap(ratingPlateBitmap, 432f, 49f, null)

            //绘制姓名板
            val nameBoxBitmap =
                drawableToBitmap(context, R.drawable.mmd_player_name_box, 302, 46)
            containerCanvas.drawBitmap(nameBoxBitmap, 432f, 110f, null)


            //绘制rating组成板
            val ratingBoxBitmap =
                drawableToBitmap(context, R.drawable.mmd_player_rating_box, 302, 41)
            containerCanvas.drawBitmap(ratingBoxBitmap, 432f, 165f, null)


            //绘制姓名
            textPaint.apply {
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textSize = 32f
                color = Color.BLACK
                letterSpacing = 0.2f
            }

            val name = if (Settings.getNickname() != "") {
                Settings.getNickname()
            } else if (SpUtil.getDivingFishNickname() != "") {
                SpUtil.getDivingFishNickname()
            } else {
                SpUtil.getUserName()
            }

            containerCanvas.drawText(
                name,
                450f,
                145f,
                textPaint
            )

            //绘制总rating
            var rating = old.sumOf { it.ra } + new.sumOf { it.ra }
            var index = 0
            while (rating > 0) {
                val digit = rating % 10
                rating /= 10

                val ratingNumBitmap = drawableToBitmap(
                    context, context.resources.getIdentifier(
                        "mmd_player_num_drating_$digit",
                        "drawable",
                        context.packageName
                    ), 28, 34
                )
                containerCanvas.drawBitmap(ratingNumBitmap, 598 - 21f * index, 58f, null)
                index++

            }


            //绘制分版本rating
            textPaint.apply {
                isFakeBoldText = false
                color = Color.BLACK
                textSize = 14f
                letterSpacing = 0f
            }
            containerCanvas.drawText(
                ("旧版本：${old.sumOf { it.ra }}   现行版本：${new.sumOf { it.ra }}"),
                480f,
                190f,
                textPaint
            )
            val time: String = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())

            PictureUtils.savePicture(
                context,
                containerBitmap,
                PictureUtils.imagePath,
                "best40_${time}"
            )
        }

    }

    private fun drawSongItem(
        context: Context,
        record: RecordEntity,
        dataList: List<SongWithChartsEntity>
    ): Bitmap {

        val find = dataList.find { it.songData.id == record.songId }
        val songContainerBitmap =
            createBitmap(ITEM_WIDTH, ITEM_HEIGHT)
        val canvas = Canvas(songContainerBitmap)

        //绘制歌曲背景板
        val ratingBoardBitmap = drawableToBitmap(
            context, record.getRatingBoard(), ITEM_WIDTH,
            ITEM_HEIGHT
        )
        canvas.drawBitmap(ratingBoardBitmap, 0f, 0f, null)


        //绘制曲封
        val jacketBitmap: Bitmap = try {
            GlideApp.with(context).asBitmap().override(158, 155).centerCrop()
                .load(MaimaiDataClient.IMAGE_BASE_URL + find!!.songData.imageUrl).submit()
                .get()
        } catch (_: Exception) {
            GlideApp.with(context).asBitmap().override(158, 155).centerCrop()
                .load(R.drawable.mmd_song_jacket_placeholder).submit().get()
        }
        canvas.drawBitmap(jacketBitmap, 21f, 24f, null)


        //绘制难度标记
        val diffDrawable =
            ContextCompat.getDrawable(context, record.getRatingDiff())
        val diffBitmap =
            createBitmap(diffDrawable!!.intrinsicWidth / 2, diffDrawable.intrinsicHeight / 2)
        val diffCanvas = Canvas(diffBitmap)
        diffDrawable.setBounds(0, 0, diffBitmap.width, diffBitmap.height)
        diffDrawable.draw(diffCanvas)
        canvas.drawBitmap(
            diffBitmap,
            (ITEM_WIDTH - diffBitmap.width - 25).toFloat(),
            30f,
            null
        )

        //绘制类型标记
        val typeBitmap = drawableToBitmap(context, record.getTypeIcon(), 96, 27)
        canvas.drawBitmap(typeBitmap, 101f, 0f, null)


        //绘制rank标记
        val rankBitmap = trim(drawableToBitmap(context, record.getRankIcon(), 100, 36))
        canvas.drawBitmap(rankBitmap, 22f, 145f, null)


        //绘制fc/fs标记
        if (record.getFcIcon() != R.drawable.mmd_player_rtsong_stub) {
            val fcBitmap = drawableToBitmap(context, record.getFcIcon(), 36, 39)

            if (record.getFsIcon() != R.drawable.mmd_player_rtsong_stub) {
                canvas.drawBitmap(fcBitmap, 115f, 145f, null)
                val fsBitmap = drawableToBitmap(context, record.getFsIcon(), 36, 39)
                canvas.drawBitmap(fsBitmap, 145f, 145f, null)
            } else {
                canvas.drawBitmap(fcBitmap, 145f, 145f, null)

            }
        }

        val textPaint = TextPaint()

        textPaint.apply {
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            textSize = 14f
            color = Color.WHITE
        }


        val title =
            TextUtils.ellipsize(record.title, textPaint, 150f, TextUtils.TruncateAt.END)
                .toString()
        val measureText = textPaint.measureText(title)
        canvas.drawText(title, (ITEM_WIDTH - measureText) / 2, 202f, textPaint)


        textPaint.textSize = 12f
        val achievement = String.format(
            context.getString(R.string.maimaidx_achievement_desc),
            record.achievements
        )
        canvas.drawText(
            "Lv:${
                record.ds
            }", 22f, 226f, textPaint
        )
        canvas.drawText(
            "Ra:${
                record.ra
            }", 140f, 226f, textPaint
        )
        canvas.drawText(
            achievement,
            (ITEM_WIDTH - textPaint.measureText(achievement)) / 2,
            226f,
            textPaint
        )


        return songContainerBitmap
    }

    private fun drawableToBitmap(
        context: Context,
        res: Int,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        var bitmap = BitmapFactory.decodeResource(context.resources, res)
        if (dstHeight != 0) {
            bitmap = bitmap.scale(dstWidth, dstHeight, false)
        }
        return bitmap
    }

    private fun getRatingPlate(context: Context, rating: Int): Bitmap {
        val res = when (rating) {
            in 0..999 -> R.drawable.mmd_rating_plate_normal
            in 1000..1999 -> R.drawable.mmd_rating_plate_blue
            in 2000..3999 -> R.drawable.mmd_rating_plate_green
            in 4000..6999 -> R.drawable.mmd_rating_plate_orange
            in 7000..9999 -> R.drawable.mmd_rating_plate_red
            in 10000..11999 -> R.drawable.mmd_rating_plate_purple
            in 12000..12999 -> R.drawable.mmd_rating_plate_bronze
            in 13000..13999 -> R.drawable.mmd_rating_plate_silver
            in 14000..14499 -> R.drawable.mmd_rating_plate_gold
            in 14500..14999 -> R.drawable.mmd_rating_plate_platinum
            else -> R.drawable.mmd_rating_plate_rainbow
        }

        return drawableToBitmap(context, res, 203, 52)


    }


    /**
     * 剪裁bitmap透明区域
     */
    private fun trim(source: Bitmap): Bitmap {
        var firstX = 0
        var firstY = 0
        var lastX = source.width
        var lastY = source.height
        val pixels = IntArray(source.width * source.height)
        source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
        loop@ for (x in 0 until source.width) {
            for (y in 0 until source.height) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    firstX = x
                    break@loop
                }
            }
        }
        loop@ for (y in 0 until source.height) {
            for (x in firstX until source.width) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    firstY = y
                    break@loop
                }
            }
        }
        loop@ for (x in source.width - 1 downTo firstX) {
            for (y in source.height - 1 downTo firstY) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    lastX = x
                    break@loop
                }
            }
        }
        loop@ for (y in source.height - 1 downTo firstY) {
            for (x in source.width - 1 downTo firstX) {
                if (pixels[x + y * source.width] != Color.TRANSPARENT) {
                    lastY = y
                    break@loop
                }
            }
        }
        return Bitmap.createBitmap(source, firstX, firstY, lastX - firstX, lastY - firstY)
    }
}


