package com.paperpig.maimaidata.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PictureUtils {
    private val savePath = Environment.DIRECTORY_PICTURES + File.separator + "maimaidata_image/"

    suspend fun savePicture(context: Context, bitmap: Bitmap, fileName: String) {
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, savePath)
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                }
                val uri =
                    context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values
                    )



                context.contentResolver.openOutputStream(uri!!).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    it?.apply {
                        flush()
                        close()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "文件已保存至${savePath}目录",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }


            } else {
                val outputDir =
                    Environment.getExternalStoragePublicDirectory(savePath)
                val imageFile = File(
                    outputDir, "${fileName}.png"
                )
                if (!outputDir.exists()) outputDir.mkdirs()
                try {
                    FileOutputStream(imageFile).use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
                        it.flush()
                        it.close()
                        MediaScannerConnection.scanFile(
                            context, arrayOf(imageFile.path),
                            null, null
                        )
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
}