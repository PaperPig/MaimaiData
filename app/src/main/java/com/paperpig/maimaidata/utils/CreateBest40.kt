package com.paperpig.maimaidata.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.paperpig.maimaidata.R
import com.paperpig.maimaidata.glide.GlideApp
import com.paperpig.maimaidata.model.Record
import com.paperpig.maimaidata.model.SongData
import com.paperpig.maimaidata.network.MaimaiDataClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap


object CreateBest40 {

    private const val itemWidth = 200
    private const val itemHeight = 250
    private const val itemPadding = 10
    private const val versionPadding = 50
    private const val headerHeight = 250
    private const val containerWidth = itemWidth * 8 + itemPadding * 8 + versionPadding
    private const val containerHeight = itemHeight * 5 + itemPadding * 5


    private val savePath = Environment.DIRECTORY_PICTURES + File.separator + "maimaidata_image/"


    suspend fun createSongInfo(
        context: Activity,
        songData: List<SongData>,
        old: List<Record>,
        new: List<Record>
    ) {
        if (old.isEmpty() || new.isEmpty()) {
            Toast.makeText(context, "尚未取得歌曲信息，请稍后重试", Toast.LENGTH_SHORT)
                .show()
            return
        }
        withContext(Dispatchers.IO) {

            val containerBitmap = drawableToBitmap(context, R.drawable.mmd_player_best40_n, containerWidth,
                containerHeight+ headerHeight)
            val containerCanvas = Canvas(containerBitmap)
            val textPaint = TextPaint()


            //绘制rating数据图
            val mainBitmap =
                Bitmap.createBitmap(containerWidth,
                    containerHeight,
                    Bitmap.Config.ARGB_8888)
            val canvas = Canvas(mainBitmap)
            //绘制旧版本乐曲
            for (i in old.indices) {
                val drawBitmap = drawSongItem(context,
                    old[i], songData)
                canvas.drawBitmap(drawBitmap!!,
                    (i % 5 * itemWidth + i % 5 * itemPadding).toFloat(),
                    (i / 5 * itemHeight + i / 5 * itemPadding).toFloat(),
                    null)
            }
            //绘制现行版本乐曲
            for (i in new.indices) {
                val drawBitmap = drawSongItem(context, new[i], songData)
                canvas.drawBitmap(drawBitmap!!,
                    (i % 3 * itemWidth + (itemWidth + itemPadding) * 5 + versionPadding + i % 3 * itemPadding).toFloat(),
                    (i / 3 * itemHeight + i / 3 * itemPadding).toFloat(),
                    null)
            }

            containerCanvas.drawBitmap(mainBitmap, 0f, 250f, null)


            //绘制姓名
            textPaint.apply {
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
                textSize = 32f
                color = Color.BLACK
                letterSpacing = 0.3f
            }
            containerCanvas.drawText(SharePreferencesUtils(context).getUserName(),
                450f,
                145f,
                textPaint)

            //绘制总rating
            textPaint.apply {
                color = Color.YELLOW
                textPaint.textSize = 25f
            }
            containerCanvas.drawText((old.sumBy { it.ra } + new.sumBy { it.ra }).toString(),
                531f,
                85f,
                textPaint)


            //绘制分版本rating
            textPaint.apply {
                color = Color.BLACK
                textSize = 14f
                letterSpacing = 0f
            }
            containerCanvas.drawText(("旧版本：${old.sumBy { it.ra }}   现行版本：${new.sumBy { it.ra }}"),
                470f,
                190f,
                textPaint)
            val time: String = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA).format(Date())


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, savePath)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "best40_${time}")
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                }
                val uri =
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values)



                context.contentResolver.openOutputStream(uri!!).use {
                    containerBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    it?.apply {
                        flush()
                        close()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context,
                                "文件已保存至${savePath}目录",
                                Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }


            } else {
                val outputDir =
                    Environment.getExternalStoragePublicDirectory(savePath)
                val imageFile = File(
                    outputDir, "best40_${time}.png")
                if (!outputDir.exists()) outputDir.mkdirs()
                try {
                    FileOutputStream(imageFile).use {
                        containerBitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
                        it.flush()
                        it.close()
                        MediaScannerConnection.scanFile(context, arrayOf(imageFile.path),
                            null, null)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "文件已保存至${savePath}目录", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }

    }

    private fun drawSongItem(
        context: Context,
        record: Record,
        data: List<SongData>
    ): Bitmap? {

        val find = data.find { it.id == record.song_id }
        val songContainerBitmap =
            Bitmap.createBitmap(itemWidth, itemHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(songContainerBitmap)

        //绘制歌曲背景板
        val ratingBoardBitmap = drawableToBitmap(context, record.getRatingBoard(), itemWidth,
            itemHeight)
        canvas.drawBitmap(ratingBoardBitmap, 0f, 0f, null)


        //绘制曲封

        val jacketBitmap = GlideApp.with(context).asBitmap().override(158, 155).centerCrop()
            .load(MaimaiDataClient.IMAGE_BASE_URL + find!!.basic_info.image_url).submit().get()
        canvas.drawBitmap(jacketBitmap, 21f, 24f, null)


        //绘制难度标记
        val diffDrawable =
            ContextCompat.getDrawable(context, record.getRatingDiff())
        val diffBitmap =
            Bitmap.createBitmap(diffDrawable!!.intrinsicWidth / 2,
                diffDrawable.intrinsicHeight / 2,
                Bitmap.Config.ARGB_8888)
        val diffCanvas = Canvas(diffBitmap)
        diffDrawable.setBounds(0, 0, diffBitmap.width, diffBitmap.height)
        diffDrawable.draw(diffCanvas)
        canvas.drawBitmap(diffBitmap,
            (itemWidth - diffBitmap.width - 25).toFloat(),
            30f,
            null)

        //绘制类型标记
        val typeBitmap = drawableToBitmap(context, record.getTypeIcon(), 96, 27)
        canvas.drawBitmap(typeBitmap, 101f, 0f, null)


        //绘制rank标记
        val rankBitmap = trim(drawableToBitmap(context, record.getRankIcon(), 106, 36))
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
        canvas.drawText(title, (itemWidth - measureText) / 2, 202f, textPaint)


        textPaint.textSize = 12f
        val achievement = String.format(context.getString(R.string.maimaidx_achievement_desc),
            record.achievements)
        canvas.drawText("Lv:${
            record.ds
        }", 22f, 226f, textPaint)
        canvas.drawText("Ra:${
            record.ra
        }", 140f, 226f, textPaint)
        canvas.drawText(achievement,
            (itemWidth - textPaint.measureText(achievement)) / 2,
            226f,
            textPaint)


        return songContainerBitmap
    }

    private fun drawableToBitmap(
        context: Context,
        res: Int,
        dstWidth: Int,
        dstHeight: Int
    ): Bitmap {
        var bitmap = BitmapFactory.decodeResource(context.resources, res)
        if (dstHeight != 0 && dstHeight != 0) {
            bitmap = Bitmap.createScaledBitmap(bitmap,
                dstWidth,
                dstHeight,
                false)
        }
        return bitmap
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


